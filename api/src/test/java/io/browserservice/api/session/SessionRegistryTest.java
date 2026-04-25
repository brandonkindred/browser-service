package io.browserservice.api.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.looksee.browser.Browser;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.error.SessionCapExceededException;
import io.browserservice.api.error.SessionNotFoundException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionRegistryTest {

    private SessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SessionRegistry(props(2));
    }

    @Test
    void acquirePermitRespectsCap() {
        registry.acquirePermit();
        registry.acquirePermit();
        assertThatThrownBy(() -> registry.acquirePermit())
                .isInstanceOf(SessionCapExceededException.class);
    }

    @Test
    void releasePermitRestoresCapacity() {
        registry.acquirePermit();
        registry.releasePermit();
        registry.acquirePermit();
        registry.acquirePermit();
        assertThat(registry.availablePermits()).isZero();
    }

    @Test
    void registerAndGetReturnSameHandle() {
        SessionHandle handle = newHandle();
        registry.acquirePermit();
        registry.register(handle);

        assertThat(registry.get(handle.id())).isSameAs(handle);
        assertThat(registry.find(handle.id())).contains(handle);
        assertThat(registry.snapshot()).contains(handle);
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    void getUnknownIdThrowsSessionNotFound() {
        assertThatThrownBy(() -> registry.get(UUID.randomUUID()))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void findUnknownReturnsEmpty() {
        assertThat(registry.find(UUID.randomUUID())).isEmpty();
    }

    @Test
    void closedHandleIsNotReturned() {
        SessionHandle handle = newHandle();
        registry.acquirePermit();
        registry.register(handle);
        handle.closeOnce();

        assertThatThrownBy(() -> registry.get(handle.id()))
                .isInstanceOf(SessionNotFoundException.class);
        assertThat(registry.find(handle.id())).isEmpty();
    }

    @Test
    void removeClosesAndReleasesPermit() {
        SessionHandle handle = newHandle();
        registry.acquirePermit();
        registry.register(handle);

        boolean removed = registry.remove(handle.id());
        assertThat(removed).isTrue();
        assertThat(registry.availablePermits()).isEqualTo(2);
        assertThat(registry.size()).isZero();
    }

    @Test
    void removeMissingReturnsFalse() {
        assertThat(registry.remove(UUID.randomUUID())).isFalse();
    }

    @Test
    void removeAlreadyClosedReturnsFalseAndDoesNotReleaseAgain() {
        SessionHandle handle = newHandle();
        registry.acquirePermit();
        registry.register(handle);
        handle.closeOnce();
        int baselinePermits = registry.availablePermits();

        boolean removed = registry.remove(handle.id());
        assertThat(removed).isFalse();
        assertThat(registry.availablePermits()).isEqualTo(baselinePermits);
    }

    @Test
    void maxConcurrentReturnsConfiguredValue() {
        assertThat(registry.maxConcurrent()).isEqualTo(2);
    }

    private static SessionHandle newHandle() {
        return SessionHandle.desktop(mock(Browser.class), BrowserType.CHROME, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
    }

    private static EngineProperties props(int max) {
        return new EngineProperties(
                new EngineProperties.SessionProps(10, 60, max, 1000),
                new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000));
    }
}
