package com.fytsmart.dashboard;

/** Monotonic-time validity, shared by the screen and pure JVM tests. */
final class GpsSpeedState {
    static final long MAX_AGE_NANOS=15_000_000_000L;
    static boolean usable(boolean hasSpeed,float metersPerSecond,long fixNanos,long nowNanos) {
        return hasSpeed && !Float.isNaN(metersPerSecond) && !Float.isInfinite(metersPerSecond)
                && !Float.isInfinite(metersPerSecond*3.6f) && metersPerSecond>=0 && fixNanos>0 && nowNanos>=fixNanos
                && nowNanos-fixNanos<=MAX_AGE_NANOS;
    }
}
