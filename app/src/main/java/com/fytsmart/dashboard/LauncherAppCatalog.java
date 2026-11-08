package com.fytsmart.dashboard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Public PackageManager API, scoped to the calling/current Android user. No OEM IPC. */
final class LauncherAppCatalog {
    static final String TAG = "FYT_AppDrawer";

    static final class App {
        final String label;
        final ComponentName component;
        App(String label, ComponentName component) {
            this.label = label;
            this.component = component;
        }
    }

    static Intent launcherQuery() {
        return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    }

    static boolean accessible(Context context, ActivityInfo info) {
        if (info == null || info.applicationInfo == null || !info.exported || !info.enabled
                || !info.applicationInfo.enabled
                || (info.applicationInfo.flags & ApplicationInfo.FLAG_SUSPENDED) != 0) return false;
        return info.permission == null || info.permission.isEmpty()
                || context.checkSelfPermission(info.permission) == PackageManager.PERMISSION_GRANTED;
    }

    static List<App> load(Context context) {
        PackageManager pm = context.getPackageManager();
        Map<String, List<ResolveInfo>> packages = new LinkedHashMap<>();
        for (ResolveInfo match : pm.queryIntentActivities(launcherQuery(), 0)) {
            ActivityInfo info = match.activityInfo;
            if (!accessible(context, info) || context.getPackageName().equals(info.packageName)) continue;
            packages.computeIfAbsent(info.packageName, unused -> new ArrayList<>()).add(match);
        }
        List<App> apps = new ArrayList<>();
        for (Map.Entry<String, List<ResolveInfo>> entry : packages.entrySet()) {
            if (Thread.currentThread().isInterrupted()) return Collections.emptyList();
            try {
                // One card per package. Prefer its normal launcher ONLY if present in the
                // accessible MAIN/LAUNCHER results; never fall back to a service/INFO entry.
                List<ResolveInfo> candidates = entry.getValue();
                candidates.sort((a, b) -> a.activityInfo.name.compareTo(b.activityInfo.name));
                ResolveInfo chosen = candidates.get(0);
                Intent normal = pm.getLaunchIntentForPackage(entry.getKey());
                ComponentName preferred = normal == null ? null : normal.getComponent();
                for (ResolveInfo candidate : candidates) {
                    if (new ComponentName(candidate.activityInfo.packageName, candidate.activityInfo.name)
                            .equals(preferred)) chosen = candidate;
                }
                CharSequence label = chosen.loadLabel(pm);
                String name = label == null ? entry.getKey() : label.toString().trim();
                if (name.isEmpty()) name = entry.getKey();
                apps.add(new App(name, new ComponentName(entry.getKey(), chosen.activityInfo.name)));
            } catch (RuntimeException disappeared) {
                Log.w(TAG, "Skipped unavailable package " + entry.getKey(), disappeared);
            }
        }
        Collator alphabet = Collator.getInstance(new Locale("es", "MX"));
        alphabet.setStrength(Collator.PRIMARY);
        apps.sort((a, b) -> {
            int order = alphabet.compare(a.label, b.label);
            return order != 0 ? order : a.component.flattenToString().compareTo(b.component.flattenToString());
        });
        return apps;
    }

    static Intent launchIntent(Context context, App app) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        ActivityInfo current = pm.getActivityInfo(app.component, 0);
        if (!accessible(context, current)) throw new SecurityException("Launcher no longer accessible");
        Intent launch = launcherQuery().setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        boolean stillLauncher = false;
        for (ResolveInfo match : pm.queryIntentActivities(launcherQuery().setPackage(app.component.getPackageName()), 0)) {
            ActivityInfo info = match.activityInfo;
            if (accessible(context, info) && new ComponentName(info.packageName, info.name).equals(app.component)) {
                stillLauncher = true;
                break;
            }
        }
        if (!stillLauncher) throw new SecurityException("Component no longer MAIN/LAUNCHER");
        return launch;
    }
}
