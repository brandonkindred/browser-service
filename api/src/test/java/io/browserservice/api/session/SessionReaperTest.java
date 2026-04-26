package io.browserservice.api.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.looksee.browser.Browser;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.persistence.BrowserSessionEntity.ClosedReason;
import io.browserservice.api.persistence.BrowserSessionTracker;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionReaperTest {

    @Test
    void reapClosesExpiredHandlesAndLeavesActiveOnes() throws Exception {
        SessionRegistry registry = new SessionRegistry(props());
        BrowserSessionTracker tracker = mock(BrowserSessionTracker.class);

        SessionHandle expired = SessionHandle.desktop(mock(Browser.class), BrowserType.CHROME,
                BrowserEnvironment.TEST, Duration.ofMillis(1), Duration.ofSeconds(60));
        SessionHandle active = SessionHandle.desktop(mock(Browser.class), BrowserType.CHROME,
                BrowserEnvironment.TEST, Duration.ofSeconds(60), Duration.ofSeconds(60));

        registry.acquirePermit();
        registry.register(expired);
        registry.acquirePermit();
        registry.register(active);

        Thread.sleep(10);

        new SessionReaper(registry, tracker).reap();

        assertThat(registry.find(expired.id())).isEmpty();
        assertThat(registry.find(active.id())).isPresent();
        verify(tracker).recordReap(expired.id(), ClosedReason.REAPED_IDLE);
        verify(tracker, never()).recordReap(active.id(), ClosedReason.REAPED_IDLE);
        verify(tracker, never()).recordReap(active.id(), ClosedReason.REAPED_ABSOLUTE);
    }

    @Test
    void reapDistinguishesAbsoluteTtlExpiry() throws Exception {
        SessionRegistry registry = new SessionRegistry(props());
        BrowserSessionTracker tracker = mock(BrowserSessionTracker.class);

        SessionHandle absoluteExpired = SessionHandle.desktop(mock(Browser.class), BrowserType.CHROME,
                BrowserEnvironment.TEST, Duration.ofSeconds(60), Duration.ofMillis(1));
        registry.acquirePermit();
        registry.register(absoluteExpired);

        Thread.sleep(10);

        new SessionReaper(registry, tracker).reap();

        verify(tracker).recordReap(absoluteExpired.id(), ClosedReason.REAPED_ABSOLUTE);
    }

    @Test
    void reapSkipsAlreadyClosedHandles() {
        SessionRegistry registry = new SessionRegistry(props());
        BrowserSessionTracker tracker = mock(BrowserSessionTracker.class);
        SessionHandle handle = SessionHandle.desktop(mock(Browser.class), BrowserType.CHROME,
                BrowserEnvironment.TEST, Duration.ofSeconds(60), Duration.ofSeconds(60));
        registry.acquirePermit();
        registry.register(handle);
        handle.closeOnce();

        new SessionReaper(registry, tracker).reap();

        assertThat(registry.find(handle.id())).isEmpty();
        verify(tracker, never()).recordReap(any(UUID.class), any(ClosedReason.class));
    }

    private static EngineProperties props() {
        return new EngineProperties(
                new EngineProperties.SessionProps(10, 60, 5, 1000),
                new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000, true, 250, true, 1000, true, 2000, 50, 16777216));
    }
}
