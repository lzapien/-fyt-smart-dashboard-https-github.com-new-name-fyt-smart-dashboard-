package com.fytsmart.dashboard;

/** One bounded recovery attempt per persisted event, independent of Android for lifecycle tests. */
final class StartupRecoveryPolicy {
    interface Store {
        String read(String key);
        boolean write(String key, String value);
    }
    static final class Gate {
        private final Store store;
        Gate(Store store) { this.store = store; }
        synchronized boolean claim(String key, String event) {
            if (event == null || event.isEmpty() || event.equals(store.read(key))) return false;
            // Claim BEFORE scheduling. Process death may omit an attempt, but cannot repeat it.
            return store.write(key, event);
        }
    }
    enum State { WAITING, REQUESTED, COMPLETE, CANCELLED, FAILED }
    enum Action { WAIT, REQUEST, STOP }
    private final long created, due, deadline;
    private long requestedAt;
    private State state = State.WAITING;

    StartupRecoveryPolicy(long now, long delayMs) {
        if (now < 0 || delayMs < 1_000 || delayMs > 15_000) throw new IllegalArgumentException("Invalid recovery timing");
        created = now; due = now + delayMs; deadline = due + 5_000;
    }
    State state() { return state; }
    void cancel() { if (state == State.WAITING || state == State.REQUESTED) state = State.CANCELLED; }
    void dashboardVisible() { if (state == State.REQUESTED) state = State.COMPLETE; }
    void launchFailed() { if (state == State.REQUESTED) state = State.FAILED; }
    Action poll(long now, boolean interactive, boolean unlocked, boolean dashboardVisible, boolean defaultHome) {
        if (state != State.WAITING && state != State.REQUESTED) return Action.STOP;
        if (now < created || !defaultHome || !interactive) { state = State.CANCELLED; return Action.STOP; }
        if (state == State.REQUESTED) {
            if (dashboardVisible) { state = State.COMPLETE; return Action.STOP; }
            if (now - requestedAt >= 2_000) { state = State.FAILED; return Action.STOP; }
            return Action.WAIT;
        }
        if (now > deadline) { state = State.FAILED; return Action.STOP; }
        if (now < due || !unlocked) return Action.WAIT;
        if (dashboardVisible) { state = State.COMPLETE; return Action.STOP; }
        state = State.REQUESTED; requestedAt = now;
        return Action.REQUEST;
    }
}
