package io.browserservice.api.webdriver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.error.SessionCapExceededException;
import io.browserservice.api.error.SessionNotFoundException;
import io.browserservice.api.session.CallerId;
import io.quarkus.scheduler.Scheduled;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Proxies W3C WebDriver protocol requests to the underlying Selenium Grid. Tracks sessions by
 * mapping the grid's WebDriver session ID to the caller who created it, enforcing auth and quotas.
 */
@Service
public class WebDriverProxyService {

  private static final Logger log = LoggerFactory.getLogger(WebDriverProxyService.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Duration SESSION_IDLE_TTL = Duration.ofMinutes(30);

  private final String gridBaseUrl;
  private final HttpClient httpClient;
  private final ConcurrentMap<String, WebDriverSession> wdSessions = new ConcurrentHashMap<>();
  private final Semaphore capacity;
  private final int maxConcurrent;

  /** Constructs the proxy service from configuration. */
  public WebDriverProxyService(EngineProperties props) {
    String urls = props.selenium().urls();
    this.gridBaseUrl = urls.contains(",") ? urls.split(",")[0].trim() : urls.trim();
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(props.selenium().connectTimeoutMs()))
            .build();
    this.maxConcurrent = props.session().maxConcurrent();
    this.capacity = new Semaphore(this.maxConcurrent);
  }

  /** Creates a new WebDriver session on the grid and tracks it for the caller. */
  public ProxyResponse createSession(byte[] body, CallerId caller) throws IOException {
    if (!capacity.tryAcquire()) {
      throw new SessionCapExceededException(maxConcurrent);
    }

    boolean releasePermit = true;
    try {
      ProxyResponse response = forwardToGrid("POST", "/session", body);

      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        String wdSessionId = extractSessionId(response.body());
        if (wdSessionId != null) {
          WebDriverSession wdSession = new WebDriverSession(wdSessionId, caller);
          wdSessions.put(wdSessionId, wdSession);
          releasePermit = false;
          log.info(
              "WebDriver session created: wdSessionId={} caller={}", wdSessionId, caller.value());
        }
      }

      return response;
    } finally {
      if (releasePermit) {
        capacity.release();
      }
    }
  }

  /** Forwards a WebDriver command to the grid after verifying session ownership. */
  public ProxyResponse forward(
      String method, String wdSessionId, String subPath, byte[] body, CallerId caller)
      throws IOException {
    WebDriverSession session = requireSessionOwner(wdSessionId, caller);
    session.touch();

    String path = "/session/" + wdSessionId;
    if (subPath != null && !subPath.isEmpty()) {
      path = path + "/" + subPath;
    }

    ProxyResponse response = forwardToGrid(method, path, body);

    if ("DELETE".equalsIgnoreCase(method)
        && (subPath == null || subPath.isEmpty())
        && response.statusCode() >= 200
        && response.statusCode() < 300) {
      if (wdSessions.remove(wdSessionId) != null) {
        capacity.release();
      }
      log.info("WebDriver session deleted: wdSessionId={}", wdSessionId);
    }

    return response;
  }

  /** Forwards the grid status request (no auth required for this endpoint). */
  public ProxyResponse forwardStatus() throws IOException {
    return forwardToGrid("GET", "/status", null);
  }

  /** Looks up a tracked WebDriver session by its grid session ID. */
  public Optional<WebDriverSession> findSession(String wdSessionId) {
    return Optional.ofNullable(wdSessions.get(wdSessionId));
  }

  /** Removes a tracked session without forwarding a delete to the grid. */
  public void removeSession(String wdSessionId) {
    if (wdSessions.remove(wdSessionId) != null) {
      capacity.release();
    }
  }

  /**
   * Periodically removes tracked sessions that have been idle longer than {@link
   * #SESSION_IDLE_TTL}. Covers the case where a client disconnects or the grid expires a session
   * out-of-band without a DELETE being proxied. Active sessions are kept alive by the {@link
   * WebDriverSession#touch()} call in {@link #forward}.
   */
  @Scheduled(every = "30s")
  void reapStaleSessions() {
    Instant cutoff = Instant.now().minus(SESSION_IDLE_TTL);
    for (var entry : wdSessions.entrySet()) {
      if (entry.getValue().lastUsedAt().isBefore(cutoff)) {
        if (wdSessions.remove(entry.getKey(), entry.getValue())) {
          capacity.release();
          log.info("reaped idle WebDriver session: wdSessionId={}", entry.getKey());
        }
      }
    }
  }

  private WebDriverSession requireSessionOwner(String wdSessionId, CallerId caller) {
    WebDriverSession session = wdSessions.get(wdSessionId);
    if (session == null) {
      throw new SessionNotFoundException(
          UUID.nameUUIDFromBytes(wdSessionId.getBytes(StandardCharsets.UTF_8)));
    }
    if (!session.owner().equals(caller)) {
      throw new io.browserservice.api.error.SessionForbiddenException(
          UUID.nameUUIDFromBytes(wdSessionId.getBytes(StandardCharsets.UTF_8)));
    }
    return session;
  }

  private ProxyResponse forwardToGrid(String method, String path, byte[] body) throws IOException {
    String url = gridBaseUrl.replaceAll("/wd/hub$", "") + path;
    HttpRequest.Builder reqBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json");

    HttpRequest.BodyPublisher bodyPublisher =
        (body != null && body.length > 0)
            ? HttpRequest.BodyPublishers.ofByteArray(body)
            : HttpRequest.BodyPublishers.noBody();

    reqBuilder.method(method.toUpperCase(Locale.ROOT), bodyPublisher);

    try {
      HttpResponse<byte[]> response =
          httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
      String contentType = response.headers().firstValue("Content-Type").orElse("application/json");
      return new ProxyResponse(response.statusCode(), response.body(), contentType);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("request interrupted", e);
    }
  }

  /**
   * Extracts the session ID from a W3C WebDriver create-session response using proper JSON parsing.
   * The W3C spec defines the response as {@code {"value": {"sessionId": "...", "capabilities":
   * {...}}}}.
   */
  static String extractSessionId(byte[] responseBody) {
    if (responseBody == null || responseBody.length == 0) {
      return null;
    }
    try {
      JsonNode root = MAPPER.readTree(responseBody);
      JsonNode value = root.path("value");
      JsonNode sessionIdNode = value.path("sessionId");
      if (sessionIdNode.isTextual()) {
        return sessionIdNode.textValue();
      }
      return null;
    } catch (IOException e) {
      log.warn("failed to parse session creation response: {}", e.getMessage());
      return null;
    }
  }
}
