package io.browserservice.api.ws;

import io.browserservice.api.session.CallerId;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class CallerIdHandshakeInterceptor implements HandshakeInterceptor {

  public static final String CALLER_ATTRIBUTE = "ws.caller";
  public static final String CONNECTION_ID_ATTRIBUTE = "ws.connectionId";
  public static final String CALLER_HEADER = "X-Caller-Id";

  private static final Logger log = LoggerFactory.getLogger(CallerIdHandshakeInterceptor.class);

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    HttpHeaders headers = request.getHeaders();
    String raw = headers.getFirst(CALLER_HEADER);
    try {
      CallerId caller = CallerId.parse(raw);
      attributes.put(CALLER_ATTRIBUTE, caller);
      attributes.put(CONNECTION_ID_ATTRIBUTE, UUID.randomUUID().toString());
      return true;
    } catch (IllegalArgumentException e) {
      log.debug("rejecting WS handshake: {}", e.getMessage());
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // no-op
  }
}
