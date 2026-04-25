package io.browserservice.api.ws.push;

import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.ws.dto.EventFrame;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Polls {@code [location.href, document.readyState]} on a slow cadence and pushes
 * {@code event{kind:"navigation.changed"}} when either value changes. Suppresses
 * duplicate consecutive ticks. {@link UnhandledAlertException} is swallowed — the
 * alert watcher will surface it.
 */
public final class NavigationWatcher implements SessionEventWatcher {

    private static final String SCRIPT = "return [location.href, document.readyState];";
    private static final Logger log = LoggerFactory.getLogger(NavigationWatcher.class);

    private final UUID sessionId;
    private final SessionHandle handle;
    private final SessionLocks locks;
    private final EventBroadcaster broadcaster;
    private final int periodMs;
    private final long lockTimeoutMs;
    private final AtomicReference<Snapshot> previous = new AtomicReference<>();

    public NavigationWatcher(UUID sessionId, SessionHandle handle, SessionLocks locks,
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
        locks.tryDoWithLock(handle, lockTimeoutMs, this::probe)
                .ifPresent(this::emitOnChange);
    }

    private Snapshot probe(SessionHandle h) {
        WebDriver driver = h.driver();
        if (!(driver instanceof JavascriptExecutor js)) {
            return null;
        }
        try {
            Object raw = js.executeScript(SCRIPT);
            if (raw instanceof List<?> list && list.size() >= 2) {
                String url = list.get(0) == null ? null : list.get(0).toString();
                String readyState = list.get(1) == null ? null : list.get(1).toString();
                return new Snapshot(url, readyState);
            }
        } catch (UnhandledAlertException e) {
            return null;
        } catch (WebDriverException e) {
            log.debug("navigation probe failed for session={}: {}", sessionId, e.toString());
        }
        return null;
    }

    private void emitOnChange(Snapshot snap) {
        Snapshot prior = previous.getAndSet(snap);
        if (Objects.equals(prior, snap)) {
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", snap.url());
        data.put("ready_state", snap.readyState());
        broadcaster.broadcast(sessionId, EventFrame.of("navigation.changed", data));
    }

    private record Snapshot(String url, String readyState) {}
}
