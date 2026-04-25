package io.browserservice.api.ws.push;

/**
 * One polling pass against the bound session. Implementations read driver state under a
 * short lock, compare against their cached previous state, and emit zero or one event
 * frame via the {@link EventBroadcaster}. Implementations must be safe to call repeatedly
 * on the shared scheduler and must never block: lock contention means "skip this tick".
 */
public interface SessionEventWatcher {

    /** Polling cadence in milliseconds. */
    int periodMs();

    /** Run one tick. Implementations should swallow recoverable errors and self-disable
     *  on persistent ones (e.g. unsupported driver capabilities). */
    void tick();

    /** Optional cleanup hook called when this watcher's session has no remaining bound
     *  WS connections (or has been reaped). Default: no-op. */
    default void onStop() {}
}
