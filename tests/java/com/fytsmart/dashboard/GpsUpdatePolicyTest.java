package com.fytsmart.dashboard;

public final class GpsUpdatePolicyTest {
    private static int passed;
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); passed++; }
    private static void stationary(GpsUpdatePolicy p) { p.onFix(true, true, 0, true, 5); }
    public static void main(String[] args) {
        GpsUpdatePolicy p = new GpsUpdatePolicy(3_000, 15_000);
        check(p.intervalMs() == 3_000, "Acquisition initially uses the moving rate");
        stationary(p); check(p.intervalMs() == 3_000, "One stationary sample is insufficient");
        stationary(p); check(p.intervalMs() == 3_000, "Two samples are insufficient");
        stationary(p); check(p.intervalMs() == 15_000, "Three accurate stationary fixes permit idle rate");
        p.onFix(true, true, 1, true, 5); check(p.intervalMs() == 15_000, "Hysteresis avoids oscillation at intermediate speeds");
        p.onFix(true, true, 1.5f, true, 5); check(p.intervalMs() == 3_000, "Movement immediately restores faster requests");
        stationary(p); stationary(p); stationary(p); p.signalLost();
        check(p.intervalMs() == 3_000, "Lost signal reacquires without inventing a speed");
        stationary(p); stationary(p); p.onFix(false, true, 0, true, 5); stationary(p);
        check(p.intervalMs() == 3_000, "Stale fixes break stationary evidence");
        for (float speed : new float[]{Float.NaN, Float.POSITIVE_INFINITY, -1}) {
            stationary(p); stationary(p); p.onFix(true, true, speed, true, 5);
            check(p.intervalMs() == 3_000, "Invalid speed never selects idle");
        }
        stationary(p); stationary(p); p.onFix(true, false, 0, true, 5);
        check(p.intervalMs() == 3_000, "A fix without speed is not evidence of stopping");
        for (float accuracy : new float[]{Float.NaN, -1, 26}) {
            stationary(p); stationary(p); p.onFix(true, true, 0, true, accuracy);
            check(p.intervalMs() == 3_000, "Poor/invalid accuracy does not prove stationary");
        }
        stationary(p); stationary(p); p.onFix(true, true, 0, false, 0);
        check(p.intervalMs() == 3_000, "Unknown accuracy is not zero-meter accuracy");
        p = new GpsUpdatePolicy(5_000, 30_000); stationary(p); stationary(p); stationary(p);
        check(p.intervalMs() == 30_000, "Configured idle request rate");
        p.onFix(true, true, 15, true, 5); check(p.intervalMs() == 5_000, "Configured moving request rate");
        for (long[] invalid : new long[][]{{1_999, 15_000}, {5_001, 15_000}, {3_000, 14_999}, {3_000, 30_001}}) {
            boolean rejected = false;
            try { new GpsUpdatePolicy(invalid[0], invalid[1]); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Out-of-range settings rejected");
        }
        System.out.println("GpsUpdatePolicy: " + passed + " checks passed; request interval is not a measurement or delivery guarantee");
    }
}
