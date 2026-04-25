package io.browserservice.api.ws.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;

class NavigationWatcherTest {

    interface ScriptingDriver extends WebDriver, JavascriptExecutor {}

    private SessionLocks locks;
    private EventBroadcaster broadcaster;
    private SessionHandle handle;
    private ScriptingDriver driver;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        locks = new SessionLocks(new EngineProperties(
                new EngineProperties.SessionProps(10, 60, 5, 5000),
                new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000, true, 250, true, 1000, true, 2000, 50, 16777216)));
        broadcaster = mock(EventBroadcaster.class);
        Browser browser = mock(Browser.class);
        driver = mock(ScriptingDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        handle = SessionHandle.desktop(browser, BrowserType.CHROME, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
        sessionId = handle.id();
    }

    @Test
    void emitsOnUrlChangeAndSuppressesIdenticalTicks() {
        when(driver.executeScript(any(String.class)))
                .thenReturn(List.of("https://example.com/a", "complete"))
                .thenReturn(List.of("https://example.com/a", "complete"))   // same — suppress
                .thenReturn(List.of("https://example.com/b", "loading"));

        NavigationWatcher watcher = new NavigationWatcher(sessionId, handle, locks, broadcaster, 2000, 50);
        watcher.tick();
        watcher.tick();
        watcher.tick();

        ArgumentCaptor<EventFrame> captor = ArgumentCaptor.forClass(EventFrame.class);
        verify(broadcaster, times(2)).broadcast(eq(sessionId), captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(f -> assertThat(f.kind()).isEqualTo("navigation.changed"));
    }

    @Test
    void swallowsUnhandledAlert() {
        when(driver.executeScript(any(String.class)))
                .thenThrow(new UnhandledAlertException("alert blocking"));

        NavigationWatcher watcher = new NavigationWatcher(sessionId, handle, locks, broadcaster, 2000, 50);
        watcher.tick();

        verify(broadcaster, never()).broadcast(any(), any());
    }
}
