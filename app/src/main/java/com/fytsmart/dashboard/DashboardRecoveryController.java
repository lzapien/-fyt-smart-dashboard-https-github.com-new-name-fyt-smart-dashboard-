package com.fytsmart.dashboard;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import java.lang.ref.WeakReference;

/** A single best-effort HOME recovery, never a foreground watchdog or a third-party app launcher. */
final class DashboardRecoveryController {
    static final String TAG = "FYT_Startup";
    private static DashboardRecoveryController instance;
    static synchronized DashboardRecoveryController get(Context context) {
        if (instance == null) instance = new DashboardRecoveryController(context.getApplicationContext());
        return instance;
    }
    private final Context context;
    private final DashboardPreferences settings;
    private final SharedPreferences runtime;
    private final StartupRecoveryPolicy.Gate gate;
    private final Handler main = new Handler(Looper.getMainLooper());
    private WeakReference<Activity> dashboard = new WeakReference<>(null);
    private boolean visible, bootUnavailableLogged;
    private StartupRecoveryPolicy window;
    private String event;
    private final Runnable poll = this::pollWindow;
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                cancel("screen_off");
                String boot = bootId();
                if (boot != null && !runtime.edit().putString("off_boot", boot)
                        .putLong("off_elapsed", SystemClock.elapsedRealtime()).commit())
                    Log.w(TAG, "Cannot persist screen-off event; wake recovery unavailable");
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) onWake();
            else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason) || "recentapps".equals(reason)) cancel("user_system_navigation");
            }
        }
    };

    private DashboardRecoveryController(Context context) {
        this.context = context; settings = new DashboardPreferences(context);
        runtime = context.getSharedPreferences("dashboard_startup_runtime", Context.MODE_PRIVATE);
        gate = new StartupRecoveryPolicy.Gate(new StartupRecoveryPolicy.Store() {
            @Override public String read(String key) { return runtime.getString(key, ""); }
            @Override public boolean write(String key, String value) { return runtime.edit().putString(key, value).commit(); }
        });
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF); filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        try {
            if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(screen, filter, Context.RECEIVER_NOT_EXPORTED);
            else context.registerReceiver(screen, filter);
        } catch (RuntimeException denied) { Log.w(TAG, "Screen receiver unavailable; wake recovery cannot be observed", denied); }
    }
    void onBoot() {
        if (!settings.recoverBoot()) return;
        String boot = bootId();
        if (gate.claim("processed_boot", boot)) arm("boot:" + boot);
    }
    void onMainResumed(Activity activity) {
        dashboard = new WeakReference<>(activity); visible = true;
        if (window != null && window.state() == StartupRecoveryPolicy.State.REQUESTED) {
            window.dashboardVisible(); finishWindow();
        }
        onBoot();
        // Covers a process recreated after SCREEN_ON, if it recorded SCREEN_OFF before death.
        onWake();
    }
    void onMainPaused(Activity activity) {
        if (dashboard.get() == activity) visible = false;
    }
    void onMainDestroyed(Activity activity) {
        if (dashboard.get() == activity) { visible = false; dashboard.clear(); }
    }
    void userInteraction() { cancel("user_interaction"); }
    void configurationChanged() {
        cancel("configuration_changed");
        // Settings apply to future events; returning from settings must not launch a new recovery.
        String boot = bootId();
        if (boot != null) {
            gate.claim("processed_boot", boot);
            if (boot.equals(runtime.getString("off_boot", "")) && runtime.getLong("off_elapsed", -1) >= 0)
                gate.claim("processed_wake", boot + ":" + runtime.getLong("off_elapsed", -1));
        }
    }

    private void onWake() {
        if (!settings.recoverWake() || !interactive()) return;
        String boot = bootId(); long off = runtime.getLong("off_elapsed", -1), now = SystemClock.elapsedRealtime();
        if (boot == null || !boot.equals(runtime.getString("off_boot", "")) || off < 0 || now - off < 5_000) return;
        String token = boot + ":" + off;
        if (gate.claim("processed_wake", token)) arm("screen_wake:" + token);
    }
    private void arm(String token) {
        if (window != null) cancel("new_event");
        event = token; window = new StartupRecoveryPolicy(SystemClock.elapsedRealtime(), settings.recoveryDelayMs());
        runtime.edit().putString("last_event", event).putString("last_status", "WAITING").apply();
        Log.i(TAG, "Recovery scheduled event=" + event + " delayMs=" + settings.recoveryDelayMs()
                + "; OEM owns Maps/NavRadio startup; no playback command");
        main.post(poll);
    }
    private void pollWindow() {
        if (window == null) return;
        boolean enabled = event.startsWith("boot:") ? settings.recoverBoot() : settings.recoverWake();
        if (!enabled) { cancel("disabled"); return; }
        StartupRecoveryPolicy.Action action = window.poll(SystemClock.elapsedRealtime(), interactive(), unlocked(), visible, isDefaultHome());
        if (action == StartupRecoveryPolicy.Action.REQUEST) {
            Intent intent = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            try {
                Activity activity = dashboard.get();
                if (activity != null && !activity.isDestroyed()) activity.startActivity(intent);
                else context.startActivity(intent);
                Log.i(TAG, "One dashboard return requested; foreground not yet confirmed event=" + event);
            } catch (RuntimeException denied) {
                window.launchFailed();
                Log.w(TAG, "Android denied dashboard recovery; no retry, overlay or HOME change", denied);
            }
        }
        if (window == null) return;
        if (window.state() == StartupRecoveryPolicy.State.WAITING || window.state() == StartupRecoveryPolicy.State.REQUESTED)
            main.postDelayed(poll, 250);
        else finishWindow();
    }
    private void finishWindow() {
        if (window == null) return;
        main.removeCallbacks(poll);
        String status = window.state().name();
        runtime.edit().putString("last_status", status).apply();
        Log.i(TAG, "Recovery finished event=" + event + " status=" + status
                + "; no further foreground requests; third-party process/playback state is not guaranteed");
        window = null;
    }
    private void cancel(String reason) {
        if (window == null) return;
        window.cancel(); Log.i(TAG, "Recovery cancelled: " + reason); finishWindow();
    }
    private String bootId() {
        try {
            int count = Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT);
            if (count >= 0) return Integer.toString(count);
        } catch (Settings.SettingNotFoundException | RuntimeException ignored) { }
        if (!bootUnavailableLogged) { bootUnavailableLogged = true; Log.w(TAG, "BOOT_COUNT unavailable; automatic recovery disabled to avoid duplicates"); }
        return null;
    }
    private boolean interactive() {
        PowerManager power = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        return power != null && power.isInteractive();
    }
    private boolean unlocked() {
        UserManager users = (UserManager)context.getSystemService(Context.USER_SERVICE);
        KeyguardManager keyguard = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
        return users != null && users.isUserUnlocked() && (keyguard == null || !keyguard.isKeyguardLocked());
    }
    private boolean isDefaultHome() {
        try {
            ResolveInfo home = context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME), android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
            return home != null && home.activityInfo != null && context.getPackageName().equals(home.activityInfo.packageName);
        } catch (RuntimeException unavailable) { return false; }
    }
}
