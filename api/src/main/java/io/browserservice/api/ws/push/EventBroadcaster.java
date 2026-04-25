package io.browserservice.api.ws.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.browserservice.api.ws.Connection;
import io.browserservice.api.ws.WsSessionConnections;
import io.browserservice.api.ws.dto.EventFrame;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

/**
 * Serializes event frames once and writes them to every connection currently bound to the
 * given session. Outbound writes go through each connection's
 * {@code ConcurrentWebSocketSessionDecorator}, the same path command responses use, so
 * watcher pushes and command responses cannot interleave on the wire.
 */
@Component
public class EventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(EventBroadcaster.class);

    private final WsSessionConnections connections;
    private final ObjectMapper mapper;

    public EventBroadcaster(WsSessionConnections connections, ObjectMapper mapper) {
        this.connections = connections;
        this.mapper = mapper;
    }

    public void broadcast(UUID sessionId, EventFrame frame) {
        String json;
        try {
            json = mapper.writeValueAsString(frame);
        } catch (Exception e) {
            log.warn("ws event serialize failed kind={}: {}", frame.kind(), e.toString());
            return;
        }
        TextMessage msg = new TextMessage(json);
        for (Connection conn : connections.snapshot(sessionId)) {
            try {
                // Same writeLock the binary-pair emitter takes — guarantees this event frame
                // never lands between a (binary-header, binary-frame) pair on the wire.
                synchronized (conn.writeLock()) {
                    conn.out().sendMessage(msg);
                }
            } catch (IOException e) {
                log.debug("ws event push failed connectionId={}: {}", conn.connectionId(), e.toString());
            }
        }
    }
}
