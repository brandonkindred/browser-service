package io.browserservice.api.ws;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
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
 * Integration tests for {@link SessionSocket} and {@link CallerIdUpgradeCheck}, validating the
 * handshake authentication contract that issue #20 calls out as acceptance criteria.
 *
 * <p>Uses the built-in Java {@link HttpClient} WebSocket client so no extra test dependency is
 * needed. Command-frame dispatch behaviour is covered indirectly by {@code CommandDispatcher}'s
 * unit-level tests (tracked under #24) and by manual probes against the running dev server.
 */
@QuarkusTest
@TestProfile(SessionSocketTest.WiringProfile.class)
class SessionSocketTest {

  @TestHTTPResource("/v1/ws/sessions")
  URI httpUri;

  private URI wsUri() {
    // @TestHTTPResource always returns http:// — convert to the ws:// scheme HttpClient requires.
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
  void handshakeRejectedWith401WhenCallerIdHeaderMissing() throws Exception {
    assertThat(handshakeStatusCode(null)).isEqualTo(401);
  }

  @Test
  void handshakeRejectedWith401WhenCallerIdHeaderBlank() throws Exception {
    assertThat(handshakeStatusCode("   ")).isEqualTo(401);
  }

  private int handshakeStatusCode(String callerIdHeader) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    WebSocket.Builder builder = client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5));
    if (callerIdHeader != null) {
      builder = builder.header("X-Caller-Id", callerIdHeader);
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
  void connectionAcceptedWithValidCallerId() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    CountingListener listener = new CountingListener();
    WebSocket socket =
        client
            .newWebSocketBuilder()
            .header("X-Caller-Id", "alice")
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
  void malformedCallerIdClosesWithCode4401() throws Exception {
    // CallerId.parse rejects whitespace inside the value; the handshake check only verifies
    // the header is non-blank, so this opens the connection and then closes 4401 in @OnOpen.
    HttpClient client = HttpClient.newHttpClient();
    CountingListener listener = new CountingListener();
    client
        .newWebSocketBuilder()
        .header("X-Caller-Id", "bad id with spaces")
        .connectTimeout(Duration.ofSeconds(5))
        .buildAsync(wsUri(), listener)
        .get(5, TimeUnit.SECONDS);

    boolean closed = listener.closeLatch.await(5, TimeUnit.SECONDS);
    if (!closed) {
      throw new TimeoutException("expected onClose within 5s");
    }
    assertThat(listener.closeStatus.get()).isEqualTo(SessionSocket.CALLER_UNIDENTIFIED);
  }

  static class SilentListener implements WebSocket.Listener {}

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
          Map.entry("browserservice.selenium.urls", "http://localhost:4444/wd/hub"));
    }
  }
}
