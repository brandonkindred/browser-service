package io.browserservice.api.webdriver;

import io.browserservice.api.session.CallerId;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Provides access to the API-key-authenticated caller identity for WebDriver proxy requests. The
 * caller is set by {@link WebDriverApiKeyFilter} as a request attribute.
 */
public final class WebDriverCallerHolder {

  static final String REQUEST_ATTRIBUTE = "io.browserservice.webdriver.caller";

  private WebDriverCallerHolder() {}

  /** Retrieves the authenticated caller from the request context. */
  public static CallerId get(ContainerRequestContext request) {
    return (CallerId) request.getProperty(REQUEST_ATTRIBUTE);
  }
}
