package com.fytsmart.dashboard;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Acceptance checks against actual installed launcher apps; no invented app entries. */
final class AppDrawerChecks {
    private final Instrumentation runner;
    AppDrawerChecks(Instrumentation runner) { this.runner = runner; }
    private int passed;
    private Activity dashboard, drawer;
    private void check(boolean valid, String name) {
        if (!valid) throw new AssertionError(name);
        passed++;
    }
    private Object field(Activity activity, String name) throws Exception {
        Field member = activity.getClass().getDeclaredField(name);
        member.setAccessible(true); return member.get(activity);
    }
    private void ui(Runnable action) { runner.runOnMainSync(action); }
    private void waitLoaded() throws Exception {
        final boolean[] loading = {true};
        for (int attempt = 0; attempt < 100 && loading[0]; attempt++) {
            SystemClock.sleep(100);
            ui(() -> { try { loading[0] = (boolean)field(drawer, "loading"); }
                catch (Exception error) { throw new RuntimeException(error); } });
        }
        check(!loading[0], "Catalog loads asynchronously within timeout");
        runner.waitForIdleSync();
    }
    private void openFromSource() throws Exception {
        Instrumentation.ActivityMonitor monitor = runner.addMonitor(AppDrawerActivity.class.getName(), null, false);
        ui(() -> dashboard.findViewById(android.R.id.content).findViewWithTag("source_button").performClick());
        drawer = runner.waitForMonitorWithTimeout(monitor, 5000);
        runner.removeMonitor(monitor);
        check(drawer != null, "SOURCE opens internal drawer");
        waitLoaded();
    }
    void run() {
        Bundle result = new Bundle();
        try {
            dashboard = runner.startActivitySync(new Intent(runner.getTargetContext(), MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            final int[] sourceBounds = new int[4];
            ui(() -> {
                View source = dashboard.findViewById(android.R.id.content).findViewWithTag("source_button");
                source.getLocationOnScreen(sourceBounds); sourceBounds[2] = source.getWidth(); sourceBounds[3] = source.getHeight();
            });
            openFromSource();
            List<LauncherAppCatalog.App> realApps = LauncherAppCatalog.load(runner.getTargetContext());
            check(!realApps.isEmpty(), "Emulator contains real launchable apps");
            final Set<String> expected = new HashSet<>();
            Collator alphabet = Collator.getInstance(new Locale("es", "MX"));
            alphabet.setStrength(Collator.PRIMARY);
            for (int i = 0; i < realApps.size(); i++) {
                LauncherAppCatalog.App app = realApps.get(i);
                check(expected.add(app.component.getPackageName()), "No duplicate package " + app.label);
                check(!app.component.getPackageName().equals(runner.getTargetContext().getPackageName()), "Dashboard excluded");
                check(LauncherAppCatalog.launchIntent(runner.getTargetContext(), app).getComponent().equals(app.component),
                        "Only verified accessible MAIN/LAUNCHER " + app.label);
                if (i > 0) check(alphabet.compare(realApps.get(i - 1).label, app.label) <= 0, "Alphabetical order");
            }
            ui(() -> {
                try {
                    GridView grid = (GridView)field(drawer, "grid");
                    check(grid.getCount() == realApps.size(), "Grid uses actual PackageManager catalog");
                    View root = drawer.findViewById(android.R.id.content).findViewWithTag("app_drawer_root");
                    check(((android.graphics.drawable.ColorDrawable)root.getBackground()).getColor() == Color.BLACK,
                            "Drawer background is true black");
                    check(grid.getNumColumns() >= 4, "Landscape grid has comfortable columns");
                    check(grid.getChildAt(0).getHeight() >= 120 * runner.getTargetContext().getResources().getDisplayMetrics().density,
                            "Comfortable touch cards");
                    EditText search = (EditText)field(drawer, "search");
                    search.setText("com.google.android.apps.maps");
                    check(grid.getCount() == 1, "Search matches real package");
                    search.setText("maps"); check(grid.getCount() == 1, "Search matches real label");
                    search.setText("zzzz_no_installed_app_zzzz");
                    check(grid.getCount() == 0, "Search produces no invented result");
                    check(((TextView)field(drawer, "empty")).getText().toString().equals("Sin resultados"), "Discreet empty search");
                    search.setText(""); check(grid.getCount() == realApps.size(), "Clear query restores actual list");
                } catch (Exception error) { throw new RuntimeException(error); }
            });
            // Exercise scrolling with real installed entries in a smaller test-only viewport.
            ui(() -> {
                try {
                    GridView grid = (GridView)field(drawer, "grid");
                    android.view.ViewGroup.LayoutParams params = grid.getLayoutParams();
                    params.height = Math.round(150 * runner.getTargetContext().getResources().getDisplayMetrics().density);
                    grid.setLayoutParams(params);
                } catch (Exception error) { throw new RuntimeException(error); }
            });
            runner.waitForIdleSync();
            ui(() -> {
                try {
                    GridView grid = (GridView)field(drawer, "grid");
                    check(grid.canScrollVertically(1), "Grid scrolls when installed apps exceed viewport");
                    grid.setSelection(grid.getCount() - 1);
                } catch (Exception error) { throw new RuntimeException(error); }
            });
            runner.waitForIdleSync();
            ui(() -> {
                try {
                    GridView grid = (GridView)field(drawer, "grid");
                    check(grid.getFirstVisiblePosition() > 0 && grid.getLastVisiblePosition() == grid.getCount() - 1,
                            "Last real installed app reachable by vertical scroll");
                    android.view.ViewGroup.LayoutParams params = grid.getLayoutParams();
                    params.height = -1; grid.setLayoutParams(params); grid.setSelection(0);
                } catch (Exception error) { throw new RuntimeException(error); }
            });
            runner.waitForIdleSync();
            final boolean[] iconsReady = {false};
            for (int attempt = 0; attempt < 100 && !iconsReady[0]; attempt++) {
                SystemClock.sleep(100);
                ui(() -> {
                    try {
                        GridView grid = (GridView)field(drawer, "grid");
                        iconsReady[0] = grid.getChildCount() > 0;
                        for (int i = 0; i < grid.getChildCount(); i++) {
                            ImageView image = (ImageView)((LinearLayout)grid.getChildAt(i)).getChildAt(0);
                            iconsReady[0] &= image.getDrawable() instanceof BitmapDrawable;
                        }
                    } catch (Exception error) { throw new RuntimeException(error); }
                });
            }
            check(iconsReady[0], "Visible original installed-app icons loaded");
            // Compare raster to actual PackageManager icon, not a fabricated or dock icon.
            ui(() -> {
                try {
                    GridView grid = (GridView)field(drawer, "grid");
                    LinearLayout tile = (LinearLayout)grid.getChildAt(0);
                    ComponentName component = ComponentName.unflattenFromString((String)tile.getTag());
                    Bitmap displayed = ((BitmapDrawable)((ImageView)tile.getChildAt(0)).getDrawable()).getBitmap();
                    android.graphics.drawable.Drawable original = runner.getTargetContext().getPackageManager().getActivityIcon(component);
                    Bitmap expectedIcon = Bitmap.createBitmap(displayed.getWidth(), displayed.getHeight(), Bitmap.Config.ARGB_8888);
                    original.setBounds(0, 0, expectedIcon.getWidth(), expectedIcon.getHeight());
                    original.draw(new android.graphics.Canvas(expectedIcon));
                    check(displayed.sameAs(expectedIcon), "Drawer renders the original installed icon");
                    // Stale item models an uninstall race, only within the separate test APK.
                    Method open = AppDrawerActivity.class.getDeclaredMethod("openApp", LauncherAppCatalog.App.class);
                    open.setAccessible(true);
                    open.invoke(drawer, new LauncherAppCatalog.App("TEST ONLY missing component",
                            new ComponentName("invalid.test.absent", "invalid.test.absent.Main")));
                } catch (Exception error) { throw new RuntimeException(error); }
            });
            SystemClock.sleep(1000); waitLoaded();
            check(!drawer.isFinishing(), "Missing app handled without closing drawer");
            runner.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); SystemClock.sleep(350); runner.waitForIdleSync();
            check(drawer.isFinishing() || drawer.isDestroyed(), "Hardware BACK returns internally to dashboard");
            ui(() -> {
                View source = dashboard.findViewById(android.R.id.content).findViewWithTag("source_button");
                int[] now = new int[2]; source.getLocationOnScreen(now);
                check(now[0] == sourceBounds[0] && now[1] == sourceBounds[1]
                        && source.getWidth() == sourceBounds[2] && source.getHeight() == sourceBounds[3], "SOURCE dimensions/position unchanged");
                LinearLayout dock = dashboard.findViewById(android.R.id.content).findViewWithTag("custom_dock");
                check(dock.getChildCount() == 5, "Five original dock cards retained");
            });
            openFromSource();
            // Explicit HOME intent validates singleTask delivery; does not select a default.
            ui(() -> dashboard.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addCategory(Intent.CATEGORY_DEFAULT).setComponent(new ComponentName(runner.getTargetContext(), MainActivity.class))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
            SystemClock.sleep(450); runner.waitForIdleSync();
            check(drawer.isFinishing() || drawer.isDestroyed(), "Explicit HOME delivery closes internal drawer stack");
            check(!dashboard.isDestroyed(), "HOME preserves dashboard instance");
            openFromSource();
            ui(() -> drawer.findViewById(android.R.id.content).findViewWithTag("drawer_back").performClick());
            SystemClock.sleep(350); runner.waitForIdleSync();
            check(drawer.isFinishing() || drawer.isDestroyed(), "Visible back button returns to dashboard");
            result.putString("stream", "AppDrawer API " + android.os.Build.VERSION.SDK_INT + ": " + passed
                    + " checks passed; " + realApps.size() + " real launcher packages; no HOME preference changed.\n");
            runner.finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "FAILED after " + passed + " checks: " + error + "\n");
            runner.finish(Activity.RESULT_CANCELED, result);
        }
    }
}
