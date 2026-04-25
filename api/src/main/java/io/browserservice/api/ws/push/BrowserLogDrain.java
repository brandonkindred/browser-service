package io.browserservice.api.ws.push;

import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.ws.dto.EventFrame;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drains {@code driver.manage().logs().get(LogType.BROWSER)} on a short cadence and
 * pushes one {@code event{kind:"console.log"}} frame per log entry.
 *
 * <p>Some drivers (Safari, Appium platforms, and Chrome sessions started without
 * {@code goog:loggingPrefs}) do not support browser logs. On the first
 * {@link UnsupportedCommandException} or equivalent driver error we self-disable for the
 * remainder of the session — no error spam, no flapping.
 */
public final class BrowserLogDrain implements SessionEventWatcher {

    private static final Logger log = LoggerFactory.getLogger(BrowserLogDrain.class);

    private final UUID sessionId;
    private final SessionHandle handle;
    private final SessionLocks locks;
    private final EventBroadcaster broadcaster;
    private final int periodMs;
    private final long lockTimeoutMs;
    private final AtomicBoolean disabled = new AtomicBoolean(false);

    public BrowserLogDrain(UUID sessionId, SessionHandle handle, SessionLocks locks,
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
        if (disabled.get()) {
            return;
        }
        // Collect entries under the session lock; broadcast OUTSIDE the lock so a slow WS
        // writer (backpressure, large fan-out) cannot delay user commands that also need
        // this lock.
        List<LogEntry> entries = locks.tryDoWithLock(handle, lockTimeoutMs, this::drain).orElse(null);
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (LogEntry entry : entries) {
            broadcaster.broadcast(sessionId, EventFrame.of("console.log", toData(entry)));
        }
    }

    private List<LogEntry> drain(SessionHandle h) {
        try {
            return h.driver().manage().logs().get(LogType.BROWSER).getAll();
        } catch (UnsupportedCommandException e) {
            disable("UnsupportedCommandException");
            return List.of();
        } catch (WebDriverException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("not implemented")
                    || msg.contains("command not found")
                    || msg.contains("unsupported")) {
                disable(e.getClass().getSimpleName() + ": " + e.getMessage());
                return List.of();
            }
            log.debug("browser log read failed for session={}: {}", sessionId, e.toString());
            return List.of();
        }
    }

    private static Map<String, Object> toData(LogEntry entry) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("level", entry.getLevel().getName());
        out.put("message", entry.getMessage());
        out.put("ts", entry.getTimestamp());
        return out;
    }

    private void disable(String reason) {
        if (disabled.compareAndSet(false, true)) {
            log.info("browser log drain disabled for session={} reason={}", sessionId, reason);
        }
    }

    boolean isDisabled() {
        return disabled.get();
    }
}
