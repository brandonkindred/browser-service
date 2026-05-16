package io.browserservice.api.config;

/**
 * Strongly-typed, immutable view of the {@code browserservice.*} configuration tree.
 *
 * <p>Intentionally a record (not a {@code @ConfigMapping} interface) so test fixtures can construct
 * ad-hoc instances via the canonical record constructor. The runtime instance is produced by {@link
 * EnginePropertiesProducer}, which reads each key off MicroProfile Config and assembles the record.
 * Keep this file pure data — no Quarkus / SmallRye annotations.
 */
public record EngineProperties(
    SessionProps session,
    SeleniumProps selenium,
    AppiumProps appium,
    BrowserStackProps browserstack,
    WebSocketProps webSocket,
    SecurityProps security) {

  public record SessionProps(
      int idleTtlSeconds, int absoluteTtlSeconds, int maxConcurrent, long lockAcquireTimeoutMs) {}

  public record SecurityProps(java.util.List<String> ssrfDenylistCidrs) {
    /** Defensive copy of the denylist; {@code null} is treated as an empty list. */
    public SecurityProps {
      ssrfDenylistCidrs =
          ssrfDenylistCidrs == null
              ? java.util.List.of()
              : java.util.List.copyOf(ssrfDenylistCidrs);
    }
  }

  public record WebSocketProps(
      int commandQueueDepth,
      int idleCloseSeconds,
      int outboundBufferKiB,
      int sendTimeLimitMs,
      boolean alertPushEnabled,
      int alertPollMs,
      boolean consolePushEnabled,
      int consolePollMs,
      boolean navigationPushEnabled,
      int navigationPollMs,
      int watcherLockTimeoutMs,
      int maxBinaryFrameBytes) {}

  public record SeleniumProps(
      String urls,
      int connectTimeoutMs,
      int readTimeoutMs,
      int maxRetries,
      boolean implicitWaitEnabled,
      int implicitWaitSeconds) {}

  public record AppiumProps(
      String urls, String platform, String deviceName, int connectTimeoutMs, int maxRetries) {}

  public record BrowserStackProps(
      boolean enabled,
      String hubUrl,
      String username,
      String accessKey,
      String os,
      String osVersion,
      String browser,
      String browserVersion,
      String project,
      String build,
      String name,
      String deviceName,
      boolean realMobile,
      boolean local,
      boolean debug) {}
}
