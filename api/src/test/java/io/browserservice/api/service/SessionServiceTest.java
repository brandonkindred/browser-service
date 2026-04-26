package io.browserservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.looksee.browser.Browser;
import com.looksee.browser.MobileDevice;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.dto.CreateSessionRequest;
import io.browserservice.api.dto.SessionListResponse;
import io.browserservice.api.dto.SessionResponse;
import io.browserservice.api.dto.SessionStateResponse;
import io.browserservice.api.error.SessionCapExceededException;
import io.browserservice.api.error.SessionNotFoundException;
import io.browserservice.api.error.UpstreamUnavailableException;
import io.browserservice.api.persistence.BrowserSessionTracker;
import io.browserservice.api.session.DriverFactory;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;

class SessionServiceTest {

    private SessionRegistry registry;
    private SessionLocks locks;
    private DriverFactory drivers;
    private BrowserSessionTracker tracker;
    private SessionService service;

    @BeforeEach
    void setUp() {
        EngineProperties props = props(2);
        registry = new SessionRegistry(props);
        locks = new SessionLocks(props);
        drivers = mock(DriverFactory.class);
        tracker = mock(BrowserSessionTracker.class);
        service = new SessionService(registry, locks, drivers, tracker, props);
    }

    @Test
    void createDesktopSessionUsesDesktopFactory() {
        Browser browser = mockBrowserWithDriver();
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        SessionResponse response = service.create(new CreateSessionRequest(
                BrowserType.CHROME, BrowserEnvironment.TEST, null));

        assertThat(response.browserType()).isEqualTo(BrowserType.CHROME);
        assertThat(response.environment()).isEqualTo(BrowserEnvironment.TEST);
        assertThat(response.sessionId()).isNotNull();
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    void createMobileSessionUsesMobileFactory() {
        MobileDevice device = mockMobileWithDriver();
        when(drivers.createMobile(BrowserType.ANDROID, BrowserEnvironment.TEST)).thenReturn(device);

        SessionResponse response = service.create(new CreateSessionRequest(
                BrowserType.ANDROID, BrowserEnvironment.TEST, 120));

        assertThat(response.browserType()).isEqualTo(BrowserType.ANDROID);
        assertThat(response.sessionId()).isNotNull();
    }

    @Test
    void createRespectsIdleTtlOverride() {
        Browser browser = mockBrowserWithDriver();
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        service.create(new CreateSessionRequest(BrowserType.CHROME, BrowserEnvironment.TEST, 42));

        // One session was stored
        assertThat(registry.snapshot()).singleElement()
                .extracting(handle -> handle.idleTtl().toSeconds())
                .isEqualTo(42L);
    }

    @Test
    void createFailureReleasesPermit() {
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST))
                .thenThrow(new UpstreamUnavailableException("nope"));

        assertThatThrownBy(() ->
                service.create(new CreateSessionRequest(BrowserType.CHROME, BrowserEnvironment.TEST, null)))
                .isInstanceOf(UpstreamUnavailableException.class);

        assertThat(registry.availablePermits()).isEqualTo(2);
    }

    @Test
    void trackerFailureClosesBrowserAndReleasesPermit() {
        Browser browser = mockBrowserWithDriver();
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(tracker).recordCreate(org.mockito.ArgumentMatchers.any(SessionHandle.class));

        assertThatThrownBy(() ->
                service.create(new CreateSessionRequest(BrowserType.CHROME, BrowserEnvironment.TEST, null)))
                .isInstanceOf(RuntimeException.class);

        verify(browser).close();
        assertThat(registry.availablePermits()).isEqualTo(2);
        assertThat(registry.size()).isZero();
    }

    @Test
    void createEnforcesCap() {
        Browser browser = mockBrowserWithDriver();
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        service.create(new CreateSessionRequest(BrowserType.CHROME, BrowserEnvironment.TEST, null));
        service.create(new CreateSessionRequest(BrowserType.CHROME, BrowserEnvironment.TEST, null));

        assertThatThrownBy(() ->
                service.create(new CreateSessionRequest(BrowserType.CHROME, BrowserEnvironment.TEST, null)))
                .isInstanceOf(SessionCapExceededException.class);
    }

    @Test
    void listReturnsAllOpenSessions() {
        Browser browser = mockBrowserWithDriver();
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        service.create(new CreateSessionRequest(BrowserType.CHROME, BrowserEnvironment.TEST, null));
        SessionListResponse list = service.list();
        assertThat(list.sessions()).hasSize(1);
    }

    @Test
    void describeReturnsStateSnapshot() {
        Browser browser = mockBrowserWithDriver();
        when(browser.getViewportScrollOffset()).thenReturn(new Point(10, 20));
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        SessionResponse created = service.create(new CreateSessionRequest(
                BrowserType.CHROME, BrowserEnvironment.TEST, null));
        SessionStateResponse state = service.describe(created.sessionId());

        assertThat(state.sessionId()).isEqualTo(created.sessionId());
        assertThat(state.viewport()).isNotNull();
        assertThat(state.scrollOffset().x()).isEqualTo(10);
        assertThat(state.scrollOffset().y()).isEqualTo(20);
    }

    @Test
    void describeUnknownIdThrowsSessionNotFound() {
        assertThatThrownBy(() -> service.describe(UUID.randomUUID()))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void describeToleratesDriverFailuresReturningNullableFields() {
        Browser browser = mockBrowserWithDriver();
        when(browser.getDriver().getCurrentUrl()).thenThrow(new RuntimeException("no url"));
        when(browser.getDriver().manage()).thenThrow(new RuntimeException("no manage"));
        when(browser.getViewportScrollOffset()).thenThrow(new RuntimeException("no scroll"));
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        SessionResponse created = service.create(new CreateSessionRequest(
                BrowserType.CHROME, BrowserEnvironment.TEST, null));
        SessionStateResponse state = service.describe(created.sessionId());

        assertThat(state.currentUrl()).isNull();
        assertThat(state.viewport()).isNull();
        assertThat(state.scrollOffset()).isNull();
    }

    @Test
    void closeRemovesSession() {
        Browser browser = mockBrowserWithDriver();
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        SessionResponse created = service.create(new CreateSessionRequest(
                BrowserType.CHROME, BrowserEnvironment.TEST, null));
        service.close(created.sessionId());

        verify(browser).close();
        assertThat(registry.size()).isZero();
    }

    @Test
    void createRecordsSessionInTracker() {
        Browser browser = mockBrowserWithDriver();
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        SessionResponse created = service.create(new CreateSessionRequest(
                BrowserType.CHROME, BrowserEnvironment.TEST, null));

        org.mockito.ArgumentCaptor<SessionHandle> captor = org.mockito.ArgumentCaptor.forClass(SessionHandle.class);
        verify(tracker).recordCreate(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(created.sessionId());
    }

    @Test
    void closeRecordsClientCloseInTracker() {
        Browser browser = mockBrowserWithDriver();
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        SessionResponse created = service.create(new CreateSessionRequest(
                BrowserType.CHROME, BrowserEnvironment.TEST, null));
        service.close(created.sessionId());

        verify(tracker).recordClientClose(created.sessionId());
    }

    @Test
    void closeUnknownIdThrowsSessionNotFound() {
        assertThatThrownBy(() -> service.close(UUID.randomUUID()))
                .isInstanceOf(SessionNotFoundException.class);
    }

    private Browser mockBrowserWithDriver() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.manage().window().getSize()).thenReturn(new Dimension(1024, 768));
        when(driver.getCurrentUrl()).thenReturn("https://example.com");
        when(browser.getViewportScrollOffset()).thenReturn(new Point(0, 0));
        return browser;
    }

    private MobileDevice mockMobileWithDriver() {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        return device;
    }

    private static EngineProperties props(int max) {
        return new EngineProperties(
                new EngineProperties.SessionProps(10, 60, max, 1000),
                new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000, true, 250, true, 1000, true, 2000, 50, 16777216));
    }
}
