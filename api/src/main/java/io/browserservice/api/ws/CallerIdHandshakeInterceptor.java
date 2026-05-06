package io.browserservice.api.ws;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.UUID;

/**
 * JSR-356 handshake configurator. Stashes the raw {@code X-Caller-Id} header into the endpoint's
 * user-properties map; {@link SessionWebSocketHandler#onOpen} parses and validates the value and
 * closes the socket with code 4401 if it is missing or malformed.
 */
public class CallerIdHandshakeInterceptor extends ServerEndpointConfig.Configurator {

  public static final String CALLER_HEADER = "X-Caller-Id";
  public static final String CALLER_HEADER_RAW_ATTRIBUTE = "ws.caller.raw";
  public static final String CALLER_ATTRIBUTE = "ws.caller";
  public static final String CONNECTION_ID_ATTRIBUTE = "ws.connectionId";

  @Override
  public void modifyHandshake(
      ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
    List<String> values = request.getHeaders().get(CALLER_HEADER);
    String raw = (values == null || values.isEmpty()) ? null : values.get(0);
    sec.getUserProperties().put(CALLER_HEADER_RAW_ATTRIBUTE, raw == null ? "" : raw);
    sec.getUserProperties().put(CONNECTION_ID_ATTRIBUTE, UUID.randomUUID().toString());
  }
}
