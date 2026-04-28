package io.browserservice.api.ws.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.looksee.browser.Browser;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.session.CallerId;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.ws.dto.EventFrame;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.Logs;

class BrowserLogDrainTest {

  private SessionLocks locks;
  private EventBroadcaster broadcaster;
  private SessionHandle handle;
  private WebDriver driver;
  private Logs driverLogs;
  private UUID sessionId;

  @BeforeEach
  void setUp() {
    locks =
        new SessionLocks(
            new EngineProperties(
                new EngineProperties.SessionProps(10, 60, 5, 5000),
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
                    16777216)));
    broadcaster = mock(EventBroadcaster.class);
    Browser browser = mock(Browser.class);
    driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    when(browser.getDriver()).thenReturn(driver);
    driverLogs = driver.manage().logs();
    handle =
        SessionHandle.desktop(
            browser,
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60));
    sessionId = handle.id();
  }

  @Test
  void emitsOneEventPerLogEntry() {
    LogEntry e1 = new LogEntry(Level.WARNING, 100L, "boom");
    LogEntry e2 = new LogEntry(Level.SEVERE, 200L, "splat");
    when(driverLogs.get(LogType.BROWSER)).thenReturn(new LogEntries(List.of(e1, e2)));

    BrowserLogDrain drain = new BrowserLogDrain(sessionId, handle, locks, broadcaster, 1000, 50);
    drain.tick();

    ArgumentCaptor<EventFrame> captor = ArgumentCaptor.forClass(EventFrame.class);
    verify(broadcaster, times(2)).broadcast(eq(sessionId), captor.capture());
    assertThat(captor.getAllValues())
        .allSatisfy(f -> assertThat(f.kind()).isEqualTo("console.log"));
  }

  @Test
  void broadcastsHappenAfterTheSessionLockIsReleased() {
    LogEntry entry = new LogEntry(Level.WARNING, 1L, "x");
    when(driverLogs.get(LogType.BROWSER)).thenReturn(new LogEntries(List.of(entry, entry)));

    // Have the broadcaster try to acquire the session lock during each call. If the
    // drain were broadcasting from inside its own lock, this would deadlock or fail.
    java.util.concurrent.atomic.AtomicInteger acquired =
        new java.util.concurrent.atomic.AtomicInteger();
    org.mockito.Mockito.doAnswer(
            inv -> {
              if (handle.lock().tryLock()) {
                try {
                  acquired.incrementAndGet();
                } finally {
                  handle.lock().unlock();
                }
              }
              return null;
            })
        .when(broadcaster)
        .broadcast(eq(sessionId), any());

    BrowserLogDrain drain = new BrowserLogDrain(sessionId, handle, locks, broadcaster, 1000, 50);
    drain.tick();

    assertThat(acquired.get()).as("broadcasts must run outside the session lock").isEqualTo(2);
  }

  @Test
  void selfDisablesOnUnsupportedCommand() {
    when(driverLogs.get(LogType.BROWSER))
        .thenThrow(new UnsupportedCommandException("not supported"));

    BrowserLogDrain drain = new BrowserLogDrain(sessionId, handle, locks, broadcaster, 1000, 50);
    drain.tick();
    drain.tick();
    drain.tick();

    assertThat(drain.isDisabled()).isTrue();
    // After the first failure, subsequent ticks must not call the driver again.
    verify(driverLogs, times(1)).get(LogType.BROWSER);
    verify(broadcaster, times(0)).broadcast(any(), any());
  }
}
