package io.browserservice.api.ws.push;

import io.browserservice.api.dto.AlertStateResponse;
import io.browserservice.api.service.AlertService;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.ws.dto.EventFrame;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls {@code driver.switchTo().alert()} on a short cadence and pushes
 * {@code event{kind:"alert.appeared"}} on the rising edge — not once per tick while the
 * alert remains visible, and not after dismissal.
 *
 * <p>Reuses {@link AlertService#peekAlert(SessionHandle)} (the lock-free helper extracted
 * for this watcher) so the alert detection stays consistent with the REST surface.
 */
public final class AlertWatcher implements SessionEventWatcher {

    private final UUID sessionId;
    private final SessionHandle handle;
    private final SessionLocks locks;
    private final EventBroadcaster broadcaster;
    private final int periodMs;
    private final long lockTimeoutMs;
    private final AtomicBoolean previouslyPresent = new AtomicBoolean(false);

    public AlertWatcher(UUID sessionId, SessionHandle handle, SessionLocks locks,
                        EventBroadcaster broadcaster, int periodMs, long lockTimeoutMs) {
        this.sessionId = sessionId;
        this.handle = handle;
        this.locks = locks;
        this.broadcaster = broadcaster;
        this.periodMs = periodMs;
        this.lockTimeoutMs = lockTimeoutMs;
    }

    @Override
    public int periodMs() {
        return periodMs;
    }

    @Override
    public void tick() {
        locks.tryDoWithLock(handle, lockTimeoutMs, AlertService::peekAlert)
                .ifPresent(this::emitOnRisingEdge);
    }

    private void emitOnRisingEdge(AlertStateResponse state) {
        boolean wasPresent = previouslyPresent.getAndSet(state.present());
        if (state.present() && !wasPresent) {
            broadcaster.broadcast(sessionId, EventFrame.of("alert.appeared",
                    Map.of("text", state.text() == null ? "" : state.text())));
        }
    }
}
