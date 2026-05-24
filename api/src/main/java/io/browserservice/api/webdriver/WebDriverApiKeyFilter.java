package io.browserservice.api.webdriver;

import io.browserservice.api.session.CallerId;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Authenticates {@code /wd/hub} requests via API key. Supports two mechanisms:
 *
 * <ul>
 *   <li><b>Basic Auth</b> (preferred for Selenium clients): username becomes the caller identity
 *       subject, password is the API key. Example: {@code http://myuser:apikey@host:8080/wd/hub}
 *   <li><b>X-API-Key header</b>: the key is looked up directly; the caller identity is derived from
 *       the key's configured owner.
 * </ul>
 *
 * <p>API keys are configured via the {@code browserservice.webdriver.api-keys} property as a
 * comma-separated list of {@code key:tenant_id:subject} triples (e.g. {@code
 * sk_live_abc:acme:user1,sk_live_def:corp:user2}).
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class WebDriverApiKeyFilter implements ContainerRequestFilter {

  private static final String WD_PATH_PREFIX = "wd/hub";
  private static final String BASIC_PREFIX = "Basic ";
  private static final String API_KEY_HEADER = "X-API-Key";

  private final ConcurrentMap<String, CallerId> keyRegistry = new ConcurrentHashMap<>();

  /** Constructs the filter from MicroProfile Config. */
  @Inject
  public WebDriverApiKeyFilter() {
    Config config = ConfigProvider.getConfig();
    String keys =
        config.getOptionalValue("browserservice.webdriver.api-keys", String.class).orElse("");
    parseApiKeys(keys);
  }

  /** Test-only constructor that accepts a raw key configuration string. */
  WebDriverApiKeyFilter(String apiKeysConfig) {
    parseApiKeys(apiKeysConfig);
  }

  @Override
  public void filter(ContainerRequestContext request) {
    String path = request.getUriInfo().getPath();
    if (!isWdPath(path) || isStatusPath(path)) {
      return;
    }

    CallerId caller = authenticate(request);
    if (caller == null) {
      request.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .header("WWW-Authenticate", "Basic realm=\"browser-service\"")
              .entity("{\"error\":\"unauthorized\",\"message\":\"valid API key required\"}")
              .type("application/json")
              .build());
      return;
    }

    request.setProperty(WebDriverCallerHolder.REQUEST_ATTRIBUTE, caller);
  }

  private CallerId authenticate(ContainerRequestContext request) {
    String authHeader = request.getHeaderString("Authorization");
    if (authHeader != null && authHeader.startsWith(BASIC_PREFIX)) {
      return authenticateBasic(authHeader.substring(BASIC_PREFIX.length()));
    }

    String apiKey = request.getHeaderString(API_KEY_HEADER);
    if (apiKey != null && !apiKey.isBlank()) {
      return keyRegistry.get(apiKey.trim());
    }

    return null;
  }

  private CallerId authenticateBasic(String encoded) {
    try {
      String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
      int colonIdx = decoded.indexOf(':');
      if (colonIdx < 0) {
        return null;
      }
      String username = decoded.substring(0, colonIdx);
      String password = decoded.substring(colonIdx + 1);
      CallerId keyCaller = keyRegistry.get(password);
      if (keyCaller == null) {
        return null;
      }
      if (username.isBlank()) {
        return keyCaller;
      }
      return CallerId.of(keyCaller.tenantId(), username);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private void parseApiKeys(String config) {
    if (config == null || config.isBlank()) {
      return;
    }
    for (String entry : config.split(",")) {
      String trimmed = entry.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      String[] parts = trimmed.split(":", 3);
      if (parts.length == 3) {
        String key = parts[0].trim();
        String tenantId = parts[1].trim();
        String subject = parts[2].trim();
        if (!key.isEmpty() && !tenantId.isEmpty() && !subject.isEmpty()) {
          keyRegistry.put(key, CallerId.of(tenantId, subject));
        }
      }
    }
  }

  private static boolean isWdPath(String path) {
    if (path == null) {
      return false;
    }
    String normalized = path.startsWith("/") ? path.substring(1) : path;
    return normalized.startsWith(WD_PATH_PREFIX);
  }

  private static boolean isStatusPath(String path) {
    if (path == null) {
      return false;
    }
    String normalized = path.startsWith("/") ? path.substring(1) : path;
    return "wd/hub/status".equals(normalized);
  }
}
