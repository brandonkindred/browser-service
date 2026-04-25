package io.browserservice.api.ws.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.looksee.browser.Browser;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.ws.dto.EventFrame;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;

class AlertWatcherTest {

    private SessionLocks locks;
    private EventBroadcaster broadcaster;
    private SessionHandle handle;
    private WebDriver driver;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        locks = new SessionLocks(new EngineProperties(
                new EngineProperties.SessionProps(10, 60, 5, 5000),
                new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000, true, 250, true, 1000, true, 2000, 50)));
        broadcaster = mock(EventBroadcaster.class);
        Browser browser = mock(Browser.class);
        driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(browser.getDriver()).thenReturn(driver);
        handle = SessionHandle.desktop(browser, BrowserType.CHROME, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
        sessionId = handle.id();
    }

    @Test
    void firesOnRisingEdgeOnly() {
        Alert alert = mock(Alert.class);
        when(alert.getText()).thenReturn("are you sure?");
        // tick 1: no alert; tick 2 & 3: alert present; tick 4: alert gone.
        when(driver.switchTo().alert())
                .thenThrow(new NoAlertPresentException("no"))
                .thenReturn(alert)
                .thenReturn(alert)
                .thenThrow(new NoAlertPresentException("no"));

        AlertWatcher watcher = new AlertWatcher(sessionId, handle, locks, broadcaster, 250, 50);
        watcher.tick(); // no alert
        watcher.tick(); // alert appears — emit
        watcher.tick(); // still present — suppress
        watcher.tick(); // gone

        ArgumentCaptor<EventFrame> captor = ArgumentCaptor.forClass(EventFrame.class);
        verify(broadcaster, times(1)).broadcast(eq(sessionId), captor.capture());
        EventFrame frame = captor.getValue();
        assertThat(frame.kind()).isEqualTo("alert.appeared");
        assertThat(frame.type()).isEqualTo("event");
    }

    @Test
    void doesNotFireWhenAlertPersists() {
        Alert alert = mock(Alert.class);
        when(driver.switchTo().alert()).thenReturn(alert);
        when(alert.getText()).thenReturn("hi");

        AlertWatcher watcher = new AlertWatcher(sessionId, handle, locks, broadcaster, 250, 50);
        watcher.tick();
        watcher.tick();
        watcher.tick();

        verify(broadcaster, times(1)).broadcast(eq(sessionId), any());
    }

    @Test
    void doesNotRefreshIdleTtl() {
        when(driver.switchTo().alert()).thenThrow(new NoAlertPresentException("no"));

        java.time.Instant before = handle.lastUsedAt();
        AlertWatcher watcher = new AlertWatcher(sessionId, handle, locks, broadcaster, 250, 50);
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        watcher.tick();
        watcher.tick();
        java.time.Instant after = handle.lastUsedAt();

        assertThat(after).isEqualTo(before);
        verify(broadcaster, never()).broadcast(any(), any());
    }

    @Test
    void skipsTickWhenLockUnavailable() {
        when(driver.switchTo().alert()).thenThrow(new NoAlertPresentException("no"));
        // Hold the session lock from another thread; the watcher's tryLock(50ms) will fail.
        Thread holder = new Thread(() -> {
            handle.lock().lock();
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            finally { handle.lock().unlock(); }
        });
        holder.setDaemon(true);
        holder.start();
        try { Thread.sleep(20); } catch (InterruptedException ignored) {}

        AlertWatcher watcher = new AlertWatcher(sessionId, handle, locks, broadcaster, 250, 20);
        watcher.tick();

        // Lock acquisition failed → no broadcast even though we'd expect rising-edge logic.
        verify(broadcaster, never()).broadcast(any(), any());
        try { holder.join(500); } catch (InterruptedException ignored) {}
    }
}
