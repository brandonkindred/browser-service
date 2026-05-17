package io.browserservice.api.ws;

import static org.assertj.core.api.Assertions.assertThat;

import io.browserservice.api.testsupport.TestTokens;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link SessionSocket} and {@link CallerIdUpgradeCheck}. The handshake auth
 * contract is the same as for REST (#89 acceptance criteria) except the token travels in the {@code
 * Sec-WebSocket-Protocol} subprotocol — browsers cannot set arbitrary headers on a WS upgrade.
 */
@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
@TestProfile(SessionSocketTest.WiringProfile.class)
class SessionSocketTest {

  @TestHTTPResource("/v1/ws/sessions")
  URI httpUri;

  private URI wsUri() {
    String s = httpUri.toString();
    if (s.startsWith("http://")) {
      return URI.create("ws://" + s.substring("http://".length()));
    }
    if (s.startsWith("https://")) {
      return URI.create("wss://" + s.substring("https://".length()));
    }
    return httpUri;
  }

  @Test
  void handshakeRejectedWith401WhenSubprotocolMissing() throws Exception {
    assertThat(handshakeStatusCode(null)).isEqualTo(401);
  }

  @Test
  void handshakeRejectedWith401WhenTokenIsTampered() throws Exception {
    String tampered = TestTokens.tampered("alice");
    assertThat(handshakeStatusCode("bearer, " + tampered)).isEqualTo(401);
  }

  @Test
  void handshakeRejectedWith401WhenTokenIsExpired() throws Exception {
    String expired = TestTokens.expired("alice");
    assertThat(handshakeStatusCode("bearer, " + expired)).isEqualTo(401);
  }

  @Test
  void handshakeRejectedWith401WhenTenantClaimMissing() throws Exception {
    String token = TestTokens.missingTenant("alice");
    assertThat(handshakeStatusCode("bearer, " + token)).isEqualTo(401);
  }

  private int handshakeStatusCode(String subprotocolHeader) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    WebSocket.Builder builder = client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5));
    if (subprotocolHeader != null) {
      // HttpClient sends the listed subprotocols via Sec-WebSocket-Protocol — we pre-split so
      // the literal "bearer" sentinel comes first, JWT second.
      String[] parts = subprotocolHeader.split(",", 2);
      builder = builder.subprotocols(parts[0].trim(), parts[1].trim());
    }
    CompletableFuture<WebSocket> fut = builder.buildAsync(wsUri(), new SilentListener());
    try {
      fut.get(5, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof WebSocketHandshakeException hse) {
        return hse.getResponse().statusCode();
      }
      throw e;
    }
    throw new AssertionError("handshake unexpectedly succeeded");
  }

  @Test
  void connectionAcceptedWithValidToken() throws Exception {
    String token = TestTokens.mint("alice");
    HttpClient client = HttpClient.newHttpClient();
    CountingListener listener = new CountingListener();
    WebSocket socket =
        client
            .newWebSocketBuilder()
            .subprotocols("bearer", token)
            .connectTimeout(Duration.ofSeconds(5))
            .buildAsync(wsUri(), listener)
            .get(5, TimeUnit.SECONDS);

    try {
      assertThat(listener.openSeen.get()).isTrue();
      assertThat(socket.isOutputClosed()).isFalse();
    } finally {
      socket.sendClose(WebSocket.NORMAL_CLOSURE, "test_done").get(2, TimeUnit.SECONDS);
    }
  }

  @Test
  void commandResponseRoundTripFromExecutorThread() throws Exception {
    // Proves WebSocketConnection.sendTextAndAwait works off the @OnTextMessage callback
    // thread. The unknown-op path is the cheapest way to drive the full receive-dispatch-
    // respond loop without touching Selenium.
    String token = TestTokens.mint("alice");
    HttpClient client = HttpClient.newHttpClient();
    TextCollectingListener listener = new TextCollectingListener();
    WebSocket socket =
        client
            .newWebSocketBuilder()
            .subprotocols("bearer", token)
            .connectTimeout(Duration.ofSeconds(5))
            .buildAsync(wsUri(), listener)
            .get(5, TimeUnit.SECONDS);

    try {
      socket
          .sendText("{\"type\":\"command\",\"id\":\"c-1\",\"op\":\"does.not.exist\"}", true)
          .get(2, TimeUnit.SECONDS);

      String frame = listener.frames.poll(5, TimeUnit.SECONDS);
      if (frame == null) {
        throw new TimeoutException("expected a response frame within 5s");
      }
      assertThat(frame).contains("\"id\":\"c-1\"").contains("\"ok\":false").contains("unknown_op");
    } finally {
      socket.sendClose(WebSocket.NORMAL_CLOSURE, "test_done").get(2, TimeUnit.SECONDS);
    }
  }

  static class SilentListener implements WebSocket.Listener {}

  static class TextCollectingListener implements WebSocket.Listener {
    final java.util.concurrent.LinkedBlockingDeque<String> frames =
        new java.util.concurrent.LinkedBlockingDeque<>();
    private final StringBuilder current = new StringBuilder();

    @Override
    public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      current.append(data);
      if (last) {
        frames.add(current.toString());
        current.setLength(0);
      }
      webSocket.request(1);
      return null;
    }
  }

  static class CountingListener implements WebSocket.Listener {
    final AtomicReference<Boolean> openSeen = new AtomicReference<>(false);
    final AtomicInteger closeStatus = new AtomicInteger(-1);
    final java.util.concurrent.CountDownLatch closeLatch =
        new java.util.concurrent.CountDownLatch(1);

    @Override
    public void onOpen(WebSocket webSocket) {
      openSeen.set(true);
      WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletableFuture<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      closeStatus.set(statusCode);
      closeLatch.countDown();
      return null;
    }
  }

  public static class WiringProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.ofEntries(
          Map.entry("quarkus.datasource.devservices.enabled", "false"),
          Map.entry("quarkus.datasource.db-kind", "h2"),
          Map.entry(
              "quarkus.datasource.jdbc.url",
              "jdbc:h2:mem:session-socket-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"),
          Map.entry("quarkus.datasource.username", "sa"),
          Map.entry("quarkus.datasource.password", ""),
          Map.entry("quarkus.flyway.migrate-at-start", "false"),
          Map.entry("quarkus.hibernate-orm.database.generation", "drop-and-create"),
          Map.entry("browserservice.selenium.urls", "http://localhost:4444/wd/hub"),
          Map.entry("smallrye.jwt.sign.key.location", "/privateKey.jwk"));
    }
  }
}
