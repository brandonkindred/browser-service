package io.browserservice.api.webdriver;

import io.browserservice.api.session.CallerId;
import java.time.Instant;

/**
 * Tracks a WebDriver session created through the proxy endpoint. Maps the grid's WebDriver session
 * ID to the caller who owns it.
 */
public record WebDriverSession(String webdriverSessionId, CallerId owner, Instant createdAt) {

  /** Convenience constructor that sets {@code createdAt} to now. */
  public WebDriverSession(String webdriverSessionId, CallerId owner) {
    this(webdriverSessionId, owner, Instant.now());
  }
}
