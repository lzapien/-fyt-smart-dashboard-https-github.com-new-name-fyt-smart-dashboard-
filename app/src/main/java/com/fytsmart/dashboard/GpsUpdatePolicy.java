package com.fytsmart.dashboard;

/** Hysteresis based on actual, fresh GPS fixes. No speed or position is synthesized. */
final class GpsUpdatePolicy {
    private final long movingMs, idleMs;
    private int stationaryFixes;
    private boolean idle;
    GpsUpdatePolicy(long movingMs, long idleMs) {
        if (movingMs < 2_000 || movingMs > 5_000 || idleMs < 15_000 || idleMs > 30_000)
            throw new IllegalArgumentException("Invalid GPS intervals");
        this.movingMs = movingMs; this.idleMs = idleMs;
    }
    long intervalMs() { return idle ? idleMs : movingMs; }
    void signalLost() { idle = false; stationaryFixes = 0; }
    void onFix(boolean fresh, boolean hasSpeed, float speed, boolean hasAccuracy, float accuracy) {
        if (!fresh || !hasSpeed || !Float.isFinite(speed) || speed < 0
                || !hasAccuracy || !Float.isFinite(accuracy) || accuracy < 0 || accuracy > 25) {
            signalLost(); return;
        }
        if (speed >= 1.5f) signalLost();
        else if (speed <= .5f) { if (++stationaryFixes >= 3) { stationaryFixes = 3; idle = true; } }
        else stationaryFixes = 0;
    }
}
