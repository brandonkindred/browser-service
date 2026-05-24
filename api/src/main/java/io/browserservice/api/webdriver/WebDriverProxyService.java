package io.browserservice.api.webdriver;

import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.error.SessionCapExceededException;
import io.browserservice.api.error.SessionNotFoundException;
import io.browserservice.api.session.CallerId;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

  private final String gridBaseUrl;
  private final HttpClient httpClient;
  private final ConcurrentMap<String, WebDriverSession> wdSessions = new ConcurrentHashMap<>();
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
  }

  /** Creates a new WebDriver session on the grid and tracks it for the caller. */
  public ProxyResponse createSession(byte[] body, CallerId caller) throws IOException {
    if (wdSessions.size() >= maxConcurrent) {
      throw new SessionCapExceededException(maxConcurrent);
    }

    ProxyResponse response = forwardToGrid("POST", "/session", body);

    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      String wdSessionId = extractSessionId(response.body());
      if (wdSessionId != null) {
        WebDriverSession wdSession = new WebDriverSession(wdSessionId, caller);
        wdSessions.put(wdSessionId, wdSession);
        log.info(
            "WebDriver session created: wdSessionId={} caller={}", wdSessionId, caller.value());
      }
    }

    return response;
  }

  /** Forwards a WebDriver command to the grid after verifying session ownership. */
  public ProxyResponse forward(
      String method, String wdSessionId, String subPath, byte[] body, CallerId caller)
      throws IOException {
    requireSessionOwner(wdSessionId, caller);

    String path = "/session/" + wdSessionId;
    if (subPath != null && !subPath.isEmpty()) {
      path = path + "/" + subPath;
    }

    ProxyResponse response = forwardToGrid(method, path, body);

    if ("DELETE".equalsIgnoreCase(method)
        && (subPath == null || subPath.isEmpty())
        && response.statusCode() >= 200
        && response.statusCode() < 300) {
      wdSessions.remove(wdSessionId);
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
    wdSessions.remove(wdSessionId);
  }

  private void requireSessionOwner(String wdSessionId, CallerId caller) {
    WebDriverSession session = wdSessions.get(wdSessionId);
    if (session == null) {
      throw new SessionNotFoundException(
          UUID.nameUUIDFromBytes(wdSessionId.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    if (!session.owner().equals(caller)) {
      throw new io.browserservice.api.error.SessionForbiddenException(
          UUID.nameUUIDFromBytes(wdSessionId.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
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

  private static String extractSessionId(byte[] responseBody) {
    if (responseBody == null || responseBody.length == 0) {
      return null;
    }
    String body = new String(responseBody, java.nio.charset.StandardCharsets.UTF_8);
    int idx = body.indexOf("\"sessionId\"");
    if (idx < 0) {
      return null;
    }
    int colonIdx = body.indexOf(':', idx);
    if (colonIdx < 0) {
      return null;
    }
    int startQuote = body.indexOf('"', colonIdx + 1);
    if (startQuote < 0) {
      return null;
    }
    int endQuote = body.indexOf('"', startQuote + 1);
    if (endQuote < 0) {
      return null;
    }
    return body.substring(startQuote + 1, endQuote);
  }
}
