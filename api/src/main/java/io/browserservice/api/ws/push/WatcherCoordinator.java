package io.browserservice.api.ws.push;

import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import io.browserservice.api.ws.Connection;
import io.browserservice.api.ws.WsSessionConnections;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Owns the lifecycle of the per-session WS event watchers (alerts, console, navigation).
 *
 * <ul>
 *   <li>Watchers start when the <strong>first</strong> WS connection binds to a session.</li>
 *   <li>Watchers stop when the <strong>last</strong> connection unbinds, or when a tick
 *       discovers the session has been removed from the registry (manual close, reaper).</li>
 *   <li>All scheduling shares the single application-wide {@code ws-scheduler}; we never
 *       allocate one scheduler per session.</li>
 * </ul>
 */
@Component
public class WatcherCoordinator {

    private static final Logger log = LoggerFactory.getLogger(WatcherCoordinator.class);

    private final WsSessionConnections connections;
    private final SessionRegistry registry;
    private final SessionLocks locks;
    private final EventBroadcaster broadcaster;
    private final ScheduledExecutorService scheduler;
    private final EngineProperties.WebSocketProps props;

    private final ConcurrentHashMap<UUID, List<ScheduledFuture<?>>> running = new ConcurrentHashMap<>();

    public WatcherCoordinator(WsSessionConnections connections,
                              SessionRegistry registry,
                              SessionLocks locks,
                              EventBroadcaster broadcaster,
                              ScheduledExecutorService webSocketScheduler,
                              EngineProperties props) {
        this.connections = connections;
        this.registry = registry;
        this.locks = locks;
        this.broadcaster = broadcaster;
        this.scheduler = webSocketScheduler;
        this.props = props.webSocket();
    }

    public void onSessionAttached(UUID sessionId, Connection conn) {
        boolean isFirst = connections.attach(sessionId, conn);
        if (!isFirst) {
            return;
        }
        SessionHandle handle = registry.find(sessionId).orElse(null);
        if (handle == null) {
            // Session vanished between bind and watcher start; clean up the entry we just added.
            connections.detach(sessionId, conn);
            return;
        }
        List<SessionEventWatcher> watchers = buildWatchers(sessionId, handle);
        if (watchers.isEmpty()) {
            return;
        }
        List<ScheduledFuture<?>> futures = new ArrayList<>(watchers.size());
        for (SessionEventWatcher w : watchers) {
            int period = Math.max(50, w.periodMs());
            ScheduledFuture<?> f = scheduler.scheduleAtFixedRate(
                    () -> safeTick(sessionId, w),
                    period, period, TimeUnit.MILLISECONDS);
            futures.add(f);
        }
        running.put(sessionId, futures);
        log.debug("ws watchers started session={} count={}", sessionId, futures.size());
    }

    public void onSessionDetached(UUID sessionId, Connection conn) {
        boolean isLast = connections.detach(sessionId, conn);
        if (isLast) {
            stopWatchers(sessionId);
        }
    }

    private List<SessionEventWatcher> buildWatchers(UUID sessionId, SessionHandle handle) {
        long lockTimeout = props.watcherLockTimeoutMs();
        List<SessionEventWatcher> out = new ArrayList<>(3);
        if (props.alertPushEnabled()) {
            out.add(new AlertWatcher(sessionId, handle, locks, broadcaster,
                    props.alertPollMs(), lockTimeout));
        }
        if (props.consolePushEnabled()) {
            out.add(new BrowserLogDrain(sessionId, handle, locks, broadcaster,
                    props.consolePollMs(), lockTimeout));
        }
        if (props.navigationPushEnabled()) {
            out.add(new NavigationWatcher(sessionId, handle, locks, broadcaster,
                    props.navigationPollMs(), lockTimeout));
        }
        return out;
    }

    private void safeTick(UUID sessionId, SessionEventWatcher watcher) {
        SessionHandle handle = registry.find(sessionId).orElse(null);
        if (handle == null || handle.isClosed()) {
            // Session has been removed (manual close or reaper). Self-cancel.
            stopWatchers(sessionId);
            return;
        }
        try {
            watcher.tick();
        } catch (Throwable t) {
            log.debug("watcher tick threw session={} watcher={}: {}",
                    sessionId, watcher.getClass().getSimpleName(), t.toString());
        }
    }

    private void stopWatchers(UUID sessionId) {
        List<ScheduledFuture<?>> futures = running.remove(sessionId);
        if (futures == null) {
            return;
        }
        for (ScheduledFuture<?> f : futures) {
            f.cancel(false);
        }
        log.debug("ws watchers stopped session={}", sessionId);
    }

    /** Test/observability hook. */
    public boolean isWatching(UUID sessionId) {
        return running.containsKey(sessionId);
    }
}
