package io.browserservice.api.ws.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.browserservice.api.ws.WsConnectionState;
import io.browserservice.api.ws.WsSessionConnections;
import io.browserservice.api.ws.dto.EventFrame;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocketConnection;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Serializes event frames once and writes them to every connection currently bound to the given
 * session. Outbound writes go through each connection's {@link
 * io.quarkus.websockets.next.WebSocketConnection}, the same path command responses use, so watcher
 * pushes and command responses cannot interleave on the wire.
 */
@Component
public class EventBroadcaster {

  private static final Logger log = LoggerFactory.getLogger(EventBroadcaster.class);

  private final WsSessionConnections connections;
  private final OpenConnections openConnections;
  private final ObjectMapper mapper;

  public EventBroadcaster(
      WsSessionConnections connections, OpenConnections openConnections, ObjectMapper mapper) {
    this.connections = connections;
    this.openConnections = openConnections;
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
    for (WsConnectionState conn : connections.snapshot(sessionId)) {
      // Look up the live connection by its Quarkus id — the @SessionScoped proxy
      // isn't usable from the scheduler thread that drives watcher ticks.
      WebSocketConnection live =
          openConnections.findByConnectionId(conn.wsConnectionId()).orElse(null);
      if (live == null) {
        continue;
      }
      try {
        // Same writeLock the binary-pair emitter takes — guarantees this event frame
        // never lands between a (binary-header, binary-frame) pair on the wire.
        synchronized (conn.writeLock()) {
          live.sendTextAndAwait(json);
        }
      } catch (Exception e) {
        log.debug("ws event push failed connectionId={}: {}", conn.connectionId(), e.toString());
      }
    }
  }
}
