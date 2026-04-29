package io.browserservice.api.session;

import com.looksee.browser.Browser;
import com.looksee.browser.MobileDevice;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SessionHandle {

  private static final Logger log = LoggerFactory.getLogger(SessionHandle.class);

  private final UUID id;
  private final CallerId owner;
  private final Browser browser; // null iff mobile session
  private final MobileDevice mobileDevice; // null iff desktop session
  private final BrowserType browserType;
  private final BrowserEnvironment environment;
  private final Instant createdAt;
  private volatile Instant lastUsedAt;
  private final Duration idleTtl;
  private final Duration absoluteTtl;
  private final ElementHandleRegistry elements;
  private final ReentrantLock lock;
  private final AtomicBoolean closed;

  private SessionHandle(
      UUID id,
      CallerId owner,
      Browser browser,
      MobileDevice mobileDevice,
      BrowserType type,
      BrowserEnvironment env,
      Duration idleTtl,
      Duration absoluteTtl) {
    this.id = id;
    this.owner = Objects.requireNonNull(owner, "owner");
    this.browser = browser;
    this.mobileDevice = mobileDevice;
    this.browserType = type;
    this.environment = env;
    this.createdAt = Instant.now();
    this.lastUsedAt = this.createdAt;
    this.idleTtl = idleTtl;
    this.absoluteTtl = absoluteTtl;
    this.elements = new ElementHandleRegistry();
    this.lock = new ReentrantLock();
    this.closed = new AtomicBoolean(false);
  }

  public static SessionHandle desktop(
      Browser browser,
      CallerId owner,
      BrowserType type,
      BrowserEnvironment env,
      Duration idleTtl,
      Duration absoluteTtl) {
    return new SessionHandle(
        UUID.randomUUID(), owner, browser, null, type, env, idleTtl, absoluteTtl);
  }

  public static SessionHandle mobile(
      MobileDevice device,
      CallerId owner,
      BrowserType type,
      BrowserEnvironment env,
      Duration idleTtl,
      Duration absoluteTtl) {
    return new SessionHandle(
        UUID.randomUUID(), owner, null, device, type, env, idleTtl, absoluteTtl);
  }

  public UUID id() {
    return id;
  }

  public CallerId owner() {
    return owner;
  }

  public BrowserType browserType() {
    return browserType;
  }

  public BrowserEnvironment environment() {
    return environment;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant lastUsedAt() {
    return lastUsedAt;
  }

  public Duration idleTtl() {
    return idleTtl;
  }

  public Duration absoluteTtl() {
    return absoluteTtl;
  }

  public ElementHandleRegistry elements() {
    return elements;
  }

  public ReentrantLock lock() {
    return lock;
  }

  public boolean isClosed() {
    return closed.get();
  }

  public boolean isMobile() {
    return mobileDevice != null;
  }

  public Browser asBrowser() {
    if (browser == null) {
      throw new IllegalStateException("session " + id + " is not a desktop session");
    }
    return browser;
  }

  public MobileDevice asMobileDevice() {
    if (mobileDevice == null) {
      throw new IllegalStateException("session " + id + " is not a mobile session");
    }
    return mobileDevice;
  }

  public WebDriver driver() {
    return mobileDevice != null ? mobileDevice.getDriver() : browser.getDriver();
  }

  /**
   * Refreshes the idle clock. Called only by {@link SessionLocks} after a successful lock
   * acquisition. Do not invoke from controllers or read paths — server-driven instrumentation must
   * not extend a session's life.
   */
  void touch() {
    this.lastUsedAt = Instant.now();
  }

  public Instant expiresAt() {
    Instant idleExpiry = lastUsedAt.plus(idleTtl);
    Instant absoluteExpiry = createdAt.plus(absoluteTtl);
    return idleExpiry.isBefore(absoluteExpiry) ? idleExpiry : absoluteExpiry;
  }

  public boolean isExpired(Instant now) {
    return now.isAfter(createdAt.plus(absoluteTtl)) || now.isAfter(lastUsedAt.plus(idleTtl));
  }

  /**
   * Reports which deadline is responsible for this session's expiry at {@code now}. Mirrors what
   * the reaper records in logs and metrics. If both deadlines have passed, the chronologically
   * earlier one wins; if neither has passed, the soonest-to-trip one is returned so callers can
   * still reason about the next reap.
   */
  public ExpiryReason expiryReason(Instant now) {
    Instant idleDeadline = lastUsedAt.plus(idleTtl);
    Instant absoluteDeadline = createdAt.plus(absoluteTtl);
    boolean idleCrossed = !now.isBefore(idleDeadline);
    boolean absoluteCrossed = !now.isBefore(absoluteDeadline);
    if (absoluteCrossed && !idleCrossed) {
      return ExpiryReason.ABSOLUTE;
    }
    if (idleCrossed && !absoluteCrossed) {
      return ExpiryReason.IDLE;
    }
    return absoluteDeadline.isBefore(idleDeadline) ? ExpiryReason.ABSOLUTE : ExpiryReason.IDLE;
  }

  public enum ExpiryReason {
    IDLE,
    ABSOLUTE;

    public String wireValue() {
      return name().toLowerCase(java.util.Locale.ROOT);
    }
  }

  public boolean closeOnce() {
    if (!closed.compareAndSet(false, true)) {
      return false;
    }
    try {
      if (mobileDevice != null) {
        mobileDevice.close();
      } else if (browser != null) {
        browser.close();
      }
    } catch (Exception e) {
      log.warn("error while closing session {}: {}", id, e.toString());
    }
    elements.clear();
    return true;
  }
}
