package io.browserservice.api.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "browserservice")
public interface EngineProperties {

  SessionProps session();

  SeleniumProps selenium();

  AppiumProps appium();

  BrowserStackProps browserstack();

  @WithName("web-socket")
  WebSocketProps webSocket();

  interface SessionProps {
    @WithDefault("300")
    int idleTtlSeconds();

    @WithDefault("1800")
    int absoluteTtlSeconds();

    @WithDefault("20")
    int maxConcurrent();

    @WithDefault("5000")
    long lockAcquireTimeoutMs();
  }

  interface WebSocketProps {
    @WithDefault("/v1/ws/sessions")
    String path();

    @WithDefault("32")
    int commandQueueDepth();

    @WithDefault("300")
    int idleCloseSeconds();

    @WithDefault("64")
    int outboundBufferKiB();

    @WithDefault("10000")
    int sendTimeLimitMs();

    @WithDefault("true")
    boolean alertPushEnabled();

    @WithDefault("250")
    int alertPollMs();

    @WithDefault("true")
    boolean consolePushEnabled();

    @WithDefault("1000")
    int consolePollMs();

    @WithDefault("true")
    boolean navigationPushEnabled();

    @WithDefault("2000")
    int navigationPollMs();

    @WithDefault("50")
    int watcherLockTimeoutMs();

    @WithDefault("16777216")
    int maxBinaryFrameBytes();
  }

  /**
   * Selenium hub configuration consumed by the engine for chrome/firefox driver creation.
   *
   * <p>{@code urls} is comma-separated, fully qualified Selenium hub URLs including scheme and
   * path, e.g. {@code http://host:4444/wd/hub}. The engine consumes entries verbatim and does not
   * rewrite scheme or append path segments.
   */
  interface SeleniumProps {
    @WithDefault("http://localhost:4444/wd/hub")
    String urls();

    @WithDefault("15000")
    int connectTimeoutMs();

    @WithDefault("60000")
    int readTimeoutMs();

    @WithDefault("3")
    int maxRetries();

    @WithDefault("false")
    boolean implicitWaitEnabled();

    @WithDefault("10")
    int implicitWaitSeconds();
  }

  /**
   * Appium server configuration consumed by the engine for mobile driver creation.
   *
   * <p>{@code urls} is comma-separated, fully qualified Appium server URLs including scheme and
   * path, e.g. {@code http://host:4723/wd/hub}. Empty disables mobile support. The engine consumes
   * entries verbatim and does not rewrite scheme or append path segments.
   */
  interface AppiumProps {
    @WithDefault("")
    String urls();

    @WithDefault("ANDROID")
    String platform();

    @WithDefault("")
    String deviceName();

    @WithDefault("60000")
    int connectTimeoutMs();

    @WithDefault("3")
    int maxRetries();
  }

  interface BrowserStackProps {
    @WithDefault("false")
    boolean enabled();

    @WithDefault("")
    String hubUrl();

    @WithDefault("")
    String username();

    @WithDefault("")
    String accessKey();

    @WithDefault("")
    String os();

    @WithDefault("")
    String osVersion();

    @WithDefault("")
    String browser();

    @WithDefault("")
    String browserVersion();

    @WithDefault("")
    String project();

    @WithDefault("")
    String build();

    @WithDefault("")
    String name();

    @WithDefault("")
    String deviceName();

    @WithDefault("true")
    boolean realMobile();

    @WithDefault("false")
    boolean local();

    @WithDefault("true")
    boolean debug();
  }
}
