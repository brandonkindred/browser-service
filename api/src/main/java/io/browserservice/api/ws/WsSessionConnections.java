package io.browserservice.api.ws;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Per-session set of WebSocket connections currently bound to that session, used so the
 * WS event watchers (alerts, console, navigation) can fan out a single tick to every
 * connected listener. The booleans returned by {@link #attach} and {@link #detach} let
 * the {@code WatcherCoordinator} start watchers exactly once on the first attach and stop
 * them exactly once on the last detach.
 */
@Component
public class WsSessionConnections {

    private final ConcurrentHashMap<UUID, Set<Connection>> bound = new ConcurrentHashMap<>();

    /** @return true iff this is the first connection bound to {@code sessionId}. */
    public boolean attach(UUID sessionId, Connection conn) {
        boolean[] firstHolder = {false};
        bound.compute(sessionId, (id, existing) -> {
            if (existing == null) {
                Set<Connection> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
                set.add(conn);
                firstHolder[0] = true;
                return set;
            }
            existing.add(conn);
            return existing;
        });
        return firstHolder[0];
    }

    /** @return true iff this was the last connection bound to {@code sessionId}. */
    public boolean detach(UUID sessionId, Connection conn) {
        boolean[] lastHolder = {false};
        bound.compute(sessionId, (id, existing) -> {
            if (existing == null) {
                return null;
            }
            existing.remove(conn);
            if (existing.isEmpty()) {
                lastHolder[0] = true;
                return null;
            }
            return existing;
        });
        return lastHolder[0];
    }

    /** Snapshot of the connections currently bound to {@code sessionId}. */
    public Collection<Connection> snapshot(UUID sessionId) {
        Set<Connection> set = bound.get(sessionId);
        return set == null ? List.of() : List.copyOf(set);
    }

    public boolean isTracked(UUID sessionId) {
        return bound.containsKey(sessionId);
    }
}
