package com.fytsmart.dashboard;

import java.util.HashMap;
import java.util.Map;

public final class StartupRecoveryPolicyTest {
    private static int passed;
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); passed++; }
    private static final class Disk implements StartupRecoveryPolicy.Store {
        final Map<String, String> data = new HashMap<>();
        boolean writable = true;
        @Override public String read(String key) { return data.get(key); }
        @Override public boolean write(String key, String value) { if (!writable) return false; data.put(key, value); return true; }
    }
    private static StartupRecoveryPolicy window() { return new StartupRecoveryPolicy(100, 5_000); }
    public static void main(String[] args) {
        Disk disk = new Disk(); StartupRecoveryPolicy.Gate gate = new StartupRecoveryPolicy.Gate(disk);
        check(!gate.claim("boot", null) && !gate.claim("boot", ""), "Missing boot identity is fail-closed");
        check(gate.claim("boot", "374"), "Cold boot claims once");
        check(!gate.claim("boot", "374"), "Repeated onResume does not claim again");
        check(!new StartupRecoveryPolicy.Gate(disk).claim("boot", "374"), "Process recreation retains the claim");
        check(gate.claim("wake", "374:8000"), "Wake is independent from boot");
        check(!new StartupRecoveryPolicy.Gate(disk).claim("wake", "374:8000"), "Recreation during wake does not repeat");
        check(gate.claim("wake", "374:16000"), "A later wake has a new identity");
        check(gate.claim("boot", "375"), "Full reboot permits one new recovery");
        disk.writable = false;
        check(!gate.claim("boot", "376"), "Failed persistence never authorizes a recovery");
        check("375".equals(disk.data.get("boot")), "Failed claim preserves previous boot");

        StartupRecoveryPolicy p = window();
        check(p.poll(100, true, true, false, true) == StartupRecoveryPolicy.Action.WAIT, "Wait for OEM initialization");
        check(p.poll(5_099, true, true, false, true) == StartupRecoveryPolicy.Action.WAIT, "No early return");
        check(p.poll(5_100, true, true, false, true) == StartupRecoveryPolicy.Action.REQUEST, "One return request at deadline");
        check(p.poll(5_350, true, true, false, true) == StartupRecoveryPolicy.Action.WAIT, "No repeated request while confirming");
        p.dashboardVisible();
        check(p.state() == StartupRecoveryPolicy.State.COMPLETE, "Lifecycle confirms foreground");
        check(p.poll(20_000, true, true, false, true) == StartupRecoveryPolicy.Action.STOP, "Later manual app opening is never interrupted");
        p = window();
        check(p.poll(5_100, true, true, true, true) == StartupRecoveryPolicy.Action.STOP
                && p.state() == StartupRecoveryPolicy.State.COMPLETE, "Already visible dashboard has no extra start");
        p = window(); p.cancel();
        check(p.poll(5_100, true, true, false, true) == StartupRecoveryPolicy.Action.STOP, "User interaction cancels pending recovery");
        p = window();
        check(p.poll(5_100, false, true, false, true) == StartupRecoveryPolicy.Action.STOP, "Screen off cancels rather than wakes device");
        p = window();
        check(p.poll(5_100, true, true, false, false) == StartupRecoveryPolicy.Action.STOP, "Changing default HOME is respected");
        p = window();
        check(p.poll(5_100, true, false, false, true) == StartupRecoveryPolicy.Action.WAIT, "Do not cover the lock screen");
        check(p.poll(10_101, true, false, false, true) == StartupRecoveryPolicy.Action.STOP
                && p.state() == StartupRecoveryPolicy.State.FAILED, "Locked initialization has a finite timeout");
        p = window(); p.poll(5_100, true, true, false, true);
        check(p.poll(7_100, true, true, false, true) == StartupRecoveryPolicy.Action.STOP
                && p.state() == StartupRecoveryPolicy.State.FAILED, "Silent Android background-launch denial is not a success");
        p = window(); p.poll(5_100, true, true, false, true); p.launchFailed();
        check(p.poll(5_350, true, true, false, true) == StartupRecoveryPolicy.Action.STOP, "Security/activity exception has no retry");
        p = window();
        check(p.poll(99, true, true, false, true) == StartupRecoveryPolicy.Action.STOP, "Monotonic clock rollback is rejected");
        System.out.println("StartupRecoveryPolicy: " + passed + " checks passed; no third-party launch or real device event simulated as evidence");
    }
}
