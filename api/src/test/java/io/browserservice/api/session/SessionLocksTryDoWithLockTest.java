package io.browserservice.api.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.looksee.browser.Browser;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SessionLocksTryDoWithLockTest {

  private SessionLocks locks;
  private SessionHandle handle;

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
    handle =
        SessionHandle.desktop(
            Mockito.mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60));
  }

  @Test
  void doesNotRefreshIdleTtl() throws InterruptedException {
    Instant before = handle.lastUsedAt();
    Thread.sleep(10);

    Optional<String> result = locks.tryDoWithLock(handle, 50, h -> "ok");

    assertThat(result).contains("ok");
    assertThat(handle.lastUsedAt()).as("watcher path must NOT bump lastUsedAt").isEqualTo(before);
  }

  @Test
  void returnsEmptyOnContention() throws InterruptedException {
    Thread holder =
        new Thread(
            () -> {
              handle.lock().lock();
              try {
                Thread.sleep(200);
              } catch (InterruptedException ignored) {
              } finally {
                handle.lock().unlock();
              }
            });
    holder.setDaemon(true);
    holder.start();
    Thread.sleep(20);

    Optional<String> result = locks.tryDoWithLock(handle, 20, h -> "ok");

    assertThat(result).isEmpty();
    holder.join(500);
  }

  @Test
  void emptyOnNullWorkResult() {
    Optional<String> result = locks.tryDoWithLock(handle, 50, h -> null);
    assertThat(result).isEmpty();
  }
}
