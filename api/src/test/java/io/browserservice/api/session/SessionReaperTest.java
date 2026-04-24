package io.browserservice.api.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.looksee.browser.Browser;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SessionReaperTest {

    @Test
    void reapClosesExpiredHandlesAndLeavesActiveOnes() throws Exception {
        SessionRegistry registry = new SessionRegistry(props());

        SessionHandle expired = SessionHandle.desktop(mock(Browser.class), BrowserType.CHROME,
                BrowserEnvironment.TEST, Duration.ofMillis(1), Duration.ofSeconds(60));
        SessionHandle active = SessionHandle.desktop(mock(Browser.class), BrowserType.CHROME,
                BrowserEnvironment.TEST, Duration.ofSeconds(60), Duration.ofSeconds(60));

        registry.acquirePermit();
        registry.register(expired);
        registry.acquirePermit();
        registry.register(active);

        Thread.sleep(10);

        new SessionReaper(registry).reap();

        assertThat(registry.find(expired.id())).isEmpty();
        assertThat(registry.find(active.id())).isPresent();
    }

    @Test
    void reapSkipsAlreadyClosedHandles() {
        SessionRegistry registry = new SessionRegistry(props());
        SessionHandle handle = SessionHandle.desktop(mock(Browser.class), BrowserType.CHROME,
                BrowserEnvironment.TEST, Duration.ofSeconds(60), Duration.ofSeconds(60));
        registry.acquirePermit();
        registry.register(handle);
        handle.closeOnce();

        new SessionReaper(registry).reap();

        assertThat(registry.find(handle.id())).isEmpty();
    }

    private static EngineProperties props() {
        return new EngineProperties(
                new EngineProperties.SessionProps(10, 60, 5, 1000),
                new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "", "", "", "", "", false, false, false));
    }
}
