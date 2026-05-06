package io.browserservice.api.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Builds the {@link EngineProperties} record from MicroProfile Config keys at startup. Defaults
 * declared here mirror what the original Spring Boot {@code @DefaultValue} annotations enforced;
 * the equivalent {@code application.yaml} migration is tracked under issue #15. The producer keeps
 * defaults centralised in code so missing YAML keys don't blow up startup, and so tests can keep
 * constructing {@link EngineProperties} via its canonical record constructor without depending on
 * any binder framework.
 */
@ApplicationScoped
public class EnginePropertiesProducer {

  /** Assembles the {@link EngineProperties} singleton at startup from MicroProfile Config. */
  @Produces
  @Singleton
  public EngineProperties engineProperties(
      // session
      @ConfigProperty(name = "browserservice.session.idle-ttl-seconds", defaultValue = "300")
          int idleTtlSeconds,
      @ConfigProperty(name = "browserservice.session.absolute-ttl-seconds", defaultValue = "1800")
          int absoluteTtlSeconds,
      @ConfigProperty(name = "browserservice.session.max-concurrent", defaultValue = "20")
          int maxConcurrent,
      @ConfigProperty(
              name = "browserservice.session.lock-acquire-timeout-ms",
              defaultValue = "5000")
          long lockAcquireTimeoutMs,
      // selenium
      @ConfigProperty(
              name = "browserservice.selenium.urls",
              defaultValue = "http://localhost:4444/wd/hub")
          String seleniumUrls,
      @ConfigProperty(name = "browserservice.selenium.connect-timeout-ms", defaultValue = "15000")
          int seleniumConnectTimeoutMs,
      @ConfigProperty(name = "browserservice.selenium.read-timeout-ms", defaultValue = "60000")
          int seleniumReadTimeoutMs,
      @ConfigProperty(name = "browserservice.selenium.max-retries", defaultValue = "3")
          int seleniumMaxRetries,
      @ConfigProperty(
              name = "browserservice.selenium.implicit-wait-enabled",
              defaultValue = "false")
          boolean seleniumImplicitWaitEnabled,
      @ConfigProperty(name = "browserservice.selenium.implicit-wait-seconds", defaultValue = "10")
          int seleniumImplicitWaitSeconds,
      // appium
      @ConfigProperty(name = "browserservice.appium.urls", defaultValue = "") String appiumUrls,
      @ConfigProperty(name = "browserservice.appium.platform", defaultValue = "ANDROID")
          String appiumPlatform,
      @ConfigProperty(name = "browserservice.appium.device-name", defaultValue = "")
          String appiumDeviceName,
      @ConfigProperty(name = "browserservice.appium.connect-timeout-ms", defaultValue = "60000")
          int appiumConnectTimeoutMs,
      @ConfigProperty(name = "browserservice.appium.max-retries", defaultValue = "3")
          int appiumMaxRetries,
      Config config) {
    return new EngineProperties(
        new EngineProperties.SessionProps(
            idleTtlSeconds, absoluteTtlSeconds, maxConcurrent, lockAcquireTimeoutMs),
        new EngineProperties.SeleniumProps(
            seleniumUrls,
            seleniumConnectTimeoutMs,
            seleniumReadTimeoutMs,
            seleniumMaxRetries,
            seleniumImplicitWaitEnabled,
            seleniumImplicitWaitSeconds),
        new EngineProperties.AppiumProps(
            appiumUrls, appiumPlatform, appiumDeviceName, appiumConnectTimeoutMs, appiumMaxRetries),
        browserstackProps(config),
        webSocketProps(config));
  }

  private static EngineProperties.BrowserStackProps browserstackProps(Config config) {
    return new EngineProperties.BrowserStackProps(
        bool(config, "browserservice.browserstack.enabled", false),
        str(config, "browserservice.browserstack.hub-url", ""),
        str(config, "browserservice.browserstack.username", ""),
        str(config, "browserservice.browserstack.access-key", ""),
        str(config, "browserservice.browserstack.os", ""),
        str(config, "browserservice.browserstack.os-version", ""),
        str(config, "browserservice.browserstack.browser", ""),
        str(config, "browserservice.browserstack.browser-version", ""),
        str(config, "browserservice.browserstack.project", ""),
        str(config, "browserservice.browserstack.build", ""),
        str(config, "browserservice.browserstack.name", ""),
        str(config, "browserservice.browserstack.device-name", ""),
        bool(config, "browserservice.browserstack.real-mobile", true),
        bool(config, "browserservice.browserstack.local", false),
        bool(config, "browserservice.browserstack.debug", true));
  }

  private static EngineProperties.WebSocketProps webSocketProps(Config config) {
    // Note: the WS path is fixed at /v1/ws/sessions by the @ServerEndpoint annotation on
    // SessionWebSocketHandler -- JSR-356 requires a compile-time constant. The historical
    // browserservice.web-socket.path config key is intentionally not exposed here because
    // honouring it would require switching to programmatic ServerApplicationConfig
    // registration. See PR #60 review for context.
    return new EngineProperties.WebSocketProps(
        intv(config, "browserservice.web-socket.command-queue-depth", 32),
        intv(config, "browserservice.web-socket.idle-close-seconds", 300),
        intv(config, "browserservice.web-socket.outbound-buffer-ki-b", 64),
        intv(config, "browserservice.web-socket.send-time-limit-ms", 10000),
        bool(config, "browserservice.web-socket.alert-push-enabled", true),
        intv(config, "browserservice.web-socket.alert-poll-ms", 250),
        bool(config, "browserservice.web-socket.console-push-enabled", true),
        intv(config, "browserservice.web-socket.console-poll-ms", 1000),
        bool(config, "browserservice.web-socket.navigation-push-enabled", true),
        intv(config, "browserservice.web-socket.navigation-poll-ms", 2000),
        intv(config, "browserservice.web-socket.watcher-lock-timeout-ms", 50),
        intv(config, "browserservice.web-socket.max-binary-frame-bytes", 16777216));
  }

  private static String str(Config c, String key, String def) {
    return c.getOptionalValue(key, String.class).orElse(def);
  }

  private static int intv(Config c, String key, int def) {
    return c.getOptionalValue(key, Integer.class).orElse(def);
  }

  private static boolean bool(Config c, String key, boolean def) {
    return c.getOptionalValue(key, Boolean.class).orElse(def);
  }
}
