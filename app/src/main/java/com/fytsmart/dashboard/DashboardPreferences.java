package com.fytsmart.dashboard;

import android.content.Context;
import android.content.SharedPreferences;

/** User choices only; runtime event claims live in a separate preference file. */
final class DashboardPreferences {
    static final String FILE = "dashboard_operation";
    static final String RECOVER_BOOT = "recover_boot", RECOVER_WAKE = "recover_wake";
    static final String GPS_CONTINUOUS = "gps_continuous", RECOVERY_DELAY = "recovery_delay_ms";
    static final String GPS_MOVING = "gps_moving_ms", GPS_IDLE = "gps_idle_ms";
    final SharedPreferences values;

    DashboardPreferences(Context context) { values = context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    boolean recoverBoot() { return values.getBoolean(RECOVER_BOOT, true); }
    boolean recoverWake() { return values.getBoolean(RECOVER_WAKE, true); }
    boolean continuousGps() { return values.getBoolean(GPS_CONTINUOUS, true); }
    long recoveryDelayMs() { return bounded(RECOVERY_DELAY, 5_000, 1_000, 15_000); }
    long movingMs() { return bounded(GPS_MOVING, 3_000, 2_000, 5_000); }
    long idleMs() { return bounded(GPS_IDLE, 15_000, 15_000, 30_000); }
    private long bounded(String key, long fallback, long min, long max) {
        long value = values.getLong(key, fallback);
        return value < min || value > max ? fallback : value;
    }
}
