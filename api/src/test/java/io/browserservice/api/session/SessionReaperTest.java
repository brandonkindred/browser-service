package io.browserservice.api.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.looksee.browser.Browser;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.persistence.BrowserSessionEntity.ClosedReason;
import io.browserservice.api.persistence.BrowserSessionTracker;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class SessionReaperTest {

  @Test
  void reapClosesExpiredHandlesAndLeavesActiveOnes() throws Exception {
    SessionRegistry registry = new SessionRegistry(props());
    BrowserSessionTracker tracker = mock(BrowserSessionTracker.class);
    MeterRegistry meters = new SimpleMeterRegistry();

    SessionHandle expired =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofMillis(1),
            Duration.ofSeconds(60));
    SessionHandle active =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(60),
            Duration.ofSeconds(60));

    registry.acquirePermit();
    registry.register(expired);
    registry.acquirePermit();
    registry.register(active);

    Thread.sleep(10);

    new SessionReaper(registry, tracker, meters).reap();

    assertThat(registry.find(expired.id())).isEmpty();
    assertThat(registry.find(active.id())).isPresent();
    verify(tracker).recordReap(expired.id(), ClosedReason.REAPED_IDLE);
    verify(tracker, never()).recordReap(active.id(), ClosedReason.REAPED_IDLE);
    verify(tracker, never()).recordReap(active.id(), ClosedReason.REAPED_ABSOLUTE);
    assertThat(reapedCount(meters, "idle")).isEqualTo(1.0);
    assertThat(reapedCount(meters, "absolute")).isZero();
  }

  @Test
  void reapDistinguishesAbsoluteTtlExpiry() throws Exception {
    SessionRegistry registry = new SessionRegistry(props());
    BrowserSessionTracker tracker = mock(BrowserSessionTracker.class);
    MeterRegistry meters = new SimpleMeterRegistry();

    SessionHandle absoluteExpired =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(60),
            Duration.ofMillis(1));
    registry.acquirePermit();
    registry.register(absoluteExpired);

    Thread.sleep(10);

    new SessionReaper(registry, tracker, meters).reap();

    verify(tracker).recordReap(absoluteExpired.id(), ClosedReason.REAPED_ABSOLUTE);
    assertThat(reapedCount(meters, "absolute")).isEqualTo(1.0);
    assertThat(reapedCount(meters, "idle")).isZero();
  }

  @Test
  void reapLogLineNamesOwnerAndReason() throws Exception {
    SessionRegistry registry = new SessionRegistry(props());
    BrowserSessionTracker tracker = mock(BrowserSessionTracker.class);
    MeterRegistry meters = new SimpleMeterRegistry();
    SessionHandle expired =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofMillis(1),
            Duration.ofSeconds(60));
    registry.acquirePermit();
    registry.register(expired);
    Thread.sleep(10);

    ch.qos.logback.classic.Logger reaperLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SessionReaper.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    reaperLogger.addAppender(appender);
    try {
      new SessionReaper(registry, tracker, meters).reap();
    } finally {
      reaperLogger.detachAppender(appender);
    }

    assertThat(appender.list)
        .anySatisfy(
            event -> {
              assertThat(event.getLevel()).isEqualTo(Level.INFO);
              assertThat(event.getFormattedMessage())
                  .contains("owner=alice")
                  .contains("reason=idle");
            });
  }

  @Test
  void reapSkipsAlreadyClosedHandles() {
    SessionRegistry registry = new SessionRegistry(props());
    BrowserSessionTracker tracker = mock(BrowserSessionTracker.class);
    MeterRegistry meters = new SimpleMeterRegistry();
    SessionHandle handle =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(60),
            Duration.ofSeconds(60));
    registry.acquirePermit();
    registry.register(handle);
    handle.closeOnce();

    new SessionReaper(registry, tracker, meters).reap();

    assertThat(registry.find(handle.id())).isEmpty();
    verify(tracker, never()).recordReap(any(UUID.class), any(ClosedReason.class));
    assertThat(reapedCount(meters, "idle")).isZero();
    assertThat(reapedCount(meters, "absolute")).isZero();
  }

  @Test
  void activeGaugeReflectsRegistrySize() {
    SessionRegistry registry = new SessionRegistry(props());
    BrowserSessionTracker tracker = mock(BrowserSessionTracker.class);
    MeterRegistry meters = new SimpleMeterRegistry();
    new SessionReaper(registry, tracker, meters);

    assertThat(activeGauge(meters)).isZero();

    SessionHandle handle =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(60),
            Duration.ofSeconds(60));
    registry.acquirePermit();
    registry.register(handle);

    assertThat(activeGauge(meters)).isEqualTo(1.0);

    registry.remove(handle.id());

    assertThat(activeGauge(meters)).isZero();
  }

  private static double reapedCount(MeterRegistry meters, String reason) {
    return meters.counter("browserservice.sessions.reaped", "reason", reason).count();
  }

  private static double activeGauge(MeterRegistry meters) {
    return meters.get("browserservice.sessions.active").gauge().value();
  }

  private static EngineProperties props() {
    return new EngineProperties(
        new EngineProperties.SessionProps(10, 60, 5, 1000),
        new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
        new EngineProperties.AppiumProps("", "", "", 0, 0),
        new EngineProperties.BrowserStackProps(
            false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
        new EngineProperties.WebSocketProps(
            "/v1/ws/sessions",
            32,
            300,
            64,
            10000,
            true,
            250,
            true,
            1000,
            true,
            2000,
            50,
            16777216));
  }
}
