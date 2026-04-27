package io.browserservice.api.ws;

import io.browserservice.api.session.CallerId;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Tracks which CallerId owns each session that was created or first attached over the WebSocket
 * surface. REST-created sessions are not registered here and therefore cannot be attached over WS
 * until full caller attribution lands (issues #2/#3/#4).
 */
@Component
public class WsSessionOwnership {

  private final ConcurrentHashMap<UUID, CallerId> owners = new ConcurrentHashMap<>();

  public void claim(UUID sessionId, CallerId owner) {
    owners.put(Objects.requireNonNull(sessionId), Objects.requireNonNull(owner));
  }

  public boolean isOwnedBy(UUID sessionId, CallerId caller) {
    CallerId owner = owners.get(sessionId);
    return owner != null && owner.equals(caller);
  }

  public boolean isTracked(UUID sessionId) {
    return owners.containsKey(sessionId);
  }

  public void release(UUID sessionId) {
    owners.remove(sessionId);
  }
}
