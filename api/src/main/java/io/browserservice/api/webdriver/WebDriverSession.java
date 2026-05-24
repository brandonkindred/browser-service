package io.browserservice.api.webdriver;

import io.browserservice.api.session.CallerId;
import java.time.Instant;

/**
 * Tracks a WebDriver session created through the proxy endpoint. Maps the grid's WebDriver session
 * ID to the caller who owns it, with a last-used timestamp for idle-based reaping.
 */
public final class WebDriverSession {

  private final String wdSessionId;
  private final CallerId sessionOwner;
  private final Instant created;
  private volatile Instant lastUsed;

  /** Creates a new tracked session with timestamps set to now. */
  public WebDriverSession(String wdSessionId, CallerId sessionOwner) {
    this.wdSessionId = wdSessionId;
    this.sessionOwner = sessionOwner;
    this.created = Instant.now();
    this.lastUsed = this.created;
  }

  /** Returns the WebDriver session ID on the grid. */
  public String webdriverSessionId() {
    return wdSessionId;
  }

  /** Returns the caller who owns this session. */
  public CallerId owner() {
    return sessionOwner;
  }

  /** Returns the instant when this session was created. */
  public Instant createdAt() {
    return created;
  }

  /** Returns the instant when this session was last used. */
  public Instant lastUsedAt() {
    return lastUsed;
  }

  /** Refreshes the last-used timestamp to now. */
  void touch() {
    this.lastUsed = Instant.now();
  }
}
