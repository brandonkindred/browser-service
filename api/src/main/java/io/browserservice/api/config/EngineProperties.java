package io.browserservice.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "browserservice")
public record EngineProperties(
    @DefaultValue SessionProps session,
    @DefaultValue SeleniumProps selenium,
    @DefaultValue AppiumProps appium,
    @DefaultValue BrowserStackProps browserstack,
    @DefaultValue WebSocketProps webSocket) {

  public record SessionProps(
      @DefaultValue("300") int idleTtlSeconds,
      @DefaultValue("1800") int absoluteTtlSeconds,
      @DefaultValue("20") int maxConcurrent,
      @DefaultValue("5000") long lockAcquireTimeoutMs) {}

  public record WebSocketProps(
      @DefaultValue("/v1/ws/sessions") String path,
      @DefaultValue("32") int commandQueueDepth,
      @DefaultValue("300") int idleCloseSeconds,
      @DefaultValue("64") int outboundBufferKiB,
      @DefaultValue("10000") int sendTimeLimitMs,
      @DefaultValue("true") boolean alertPushEnabled,
      @DefaultValue("250") int alertPollMs,
      @DefaultValue("true") boolean consolePushEnabled,
      @DefaultValue("1000") int consolePollMs,
      @DefaultValue("true") boolean navigationPushEnabled,
      @DefaultValue("2000") int navigationPollMs,
      @DefaultValue("50") int watcherLockTimeoutMs,
      @DefaultValue("16777216") int maxBinaryFrameBytes) {}

  /**
   * Selenium hub configuration consumed by the engine for chrome/firefox driver creation.
   *
   * @param urls comma-separated, fully qualified Selenium hub URLs including scheme and path, e.g.
   *     {@code http://host:4444/wd/hub}. The engine consumes entries verbatim and does not rewrite
   *     scheme or append path segments.
   */
  public record SeleniumProps(
      @DefaultValue("http://localhost:4444/wd/hub") String urls,
      @DefaultValue("15000") int connectTimeoutMs,
      @DefaultValue("60000") int readTimeoutMs,
      @DefaultValue("3") int maxRetries,
      @DefaultValue("false") boolean implicitWaitEnabled,
      @DefaultValue("10") int implicitWaitSeconds) {}

  /**
   * Appium server configuration consumed by the engine for mobile driver creation.
   *
   * @param urls comma-separated, fully qualified Appium server URLs including scheme and path, e.g.
   *     {@code http://host:4723/wd/hub}. Empty disables mobile support. The engine consumes entries
   *     verbatim and does not rewrite scheme or append path segments.
   */
  public record AppiumProps(
      @DefaultValue("") String urls,
      @DefaultValue("ANDROID") String platform,
      @DefaultValue("") String deviceName,
      @DefaultValue("60000") int connectTimeoutMs,
      @DefaultValue("3") int maxRetries) {}

  public record BrowserStackProps(
      @DefaultValue("false") boolean enabled,
      @DefaultValue("") String hubUrl,
      @DefaultValue("") String username,
      @DefaultValue("") String accessKey,
      @DefaultValue("") String os,
      @DefaultValue("") String osVersion,
      @DefaultValue("") String browser,
      @DefaultValue("") String browserVersion,
      @DefaultValue("") String project,
      @DefaultValue("") String build,
      @DefaultValue("") String name,
      @DefaultValue("") String deviceName,
      @DefaultValue("true") boolean realMobile,
      @DefaultValue("false") boolean local,
      @DefaultValue("true") boolean debug) {}
}
