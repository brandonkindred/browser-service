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
import io.browserservice.api.dto.DomRemovePreset;
import io.browserservice.api.dto.DomRemoveRequest;
import io.browserservice.api.dto.ExecuteRequest;
import io.browserservice.api.dto.ExecuteResponse;
import io.browserservice.api.dto.MouseMoveMode;
import io.browserservice.api.dto.MouseMoveRequest;
import io.browserservice.api.dto.NavigateRequest;
import io.browserservice.api.dto.NavigateResponse;
import io.browserservice.api.dto.NavigateStatus;
import io.browserservice.api.dto.PageSourceResponse;
import io.browserservice.api.dto.PageStatusResponse;
import io.browserservice.api.dto.ScreenshotStrategy;
import io.browserservice.api.dto.ScrollMode;
import io.browserservice.api.dto.ScrollOffset;
import io.browserservice.api.dto.ScrollRequest;
import io.browserservice.api.dto.ViewportStateResponse;
import io.browserservice.api.error.DesktopSessionRequiredException;
import io.browserservice.api.error.ElementHandleNotFoundException;
import io.browserservice.api.error.ValidationFailedException;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

class BrowserOperationsServiceTest {

    private SessionRegistry registry;
    private SessionLocks locks;
    private BrowserOperationsService service;

    @BeforeEach
    void setUp() {
        EngineProperties props = props();
        registry = new SessionRegistry(props);
        locks = new SessionLocks(props);
        service = new BrowserOperationsService(registry, locks);
    }

    @Test
    void navigateDesktopSuccessReturnsLoaded() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("https://example.com");
        UUID id = register(browser);

        NavigateResponse resp = service.navigate(id, new NavigateRequest("https://example.com", null));

        assertThat(resp.status()).isEqualTo(NavigateStatus.LOADED);
        assertThat(resp.currentUrl()).isEqualTo("https://example.com");
        verify(browser).navigateTo("https://example.com");
        verify(browser).waitForPageToLoad();
    }

    @Test
    void navigateMobileSuccessReturnsLoaded() {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("https://m.example.com");
        UUID id = registerMobile(device);

        NavigateResponse resp = service.navigate(id, new NavigateRequest("https://m.example.com", null));
        assertThat(resp.status()).isEqualTo(NavigateStatus.LOADED);
    }

    @Test
    void navigateTimeoutReturnsTimeoutStatus() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("https://example.com");
        org.mockito.Mockito.doThrow(new TimeoutException("slow")).when(browser).waitForPageToLoad();
        UUID id = register(browser);

        NavigateResponse resp = service.navigate(id, new NavigateRequest("https://example.com", null));
        assertThat(resp.status()).isEqualTo(NavigateStatus.TIMEOUT);
    }

    @Test
    void navigateOtherRuntimeReturnsError() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn(null);
        org.mockito.Mockito.doThrow(new RuntimeException("broken")).when(browser).navigateTo("x");
        UUID id = register(browser);

        NavigateResponse resp = service.navigate(id, new NavigateRequest("x", null));
        assertThat(resp.status()).isEqualTo(NavigateStatus.ERROR);
    }

    @Test
    void getSourceDesktopReturnsHtml() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("u");
        when(browser.getSource()).thenReturn("<html/>");
        UUID id = register(browser);

        PageSourceResponse resp = service.getSource(id);
        assertThat(resp.source()).isEqualTo("<html/>");
        assertThat(resp.currentUrl()).isEqualTo("u");
    }

    @Test
    void getSourceMobileReturnsHtml() {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("u");
        when(device.getSource()).thenReturn("<html/>");
        UUID id = registerMobile(device);

        PageSourceResponse resp = service.getSource(id);
        assertThat(resp.source()).isEqualTo("<html/>");
    }

    @Test
    void getStatusTrueWhen503() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("u");
        when(browser.getSource()).thenReturn("<html><title>503 Service Temporarily Unavailable</title></html>");
        UUID id = register(browser);

        PageStatusResponse resp = service.getStatus(id);
        assertThat(resp.is503()).isTrue();
    }

    @Test
    void getStatusFalseWhenSourceNot503() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("u");
        when(browser.getSource()).thenReturn("<html><body>ok</body></html>");
        UUID id = register(browser);

        PageStatusResponse resp = service.getStatus(id);
        assertThat(resp.is503()).isFalse();
    }

    @Test
    void getStatusFalseWhenSourceUnavailable() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("u");
        when(browser.getSource()).thenThrow(new RuntimeException("nope"));
        UUID id = register(browser);

        assertThat(service.getStatus(id).is503()).isFalse();
    }

    @Test
    void getViewportReturnsSizeAndScroll() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.manage().window().getSize()).thenReturn(new Dimension(800, 600));
        when(browser.getViewportScrollOffset()).thenReturn(new Point(5, 10));
        UUID id = register(browser);

        ViewportStateResponse resp = service.getViewport(id);
        assertThat(resp.viewport().width()).isEqualTo(800);
        assertThat(resp.viewport().height()).isEqualTo(600);
        assertThat(resp.scrollOffset().x()).isEqualTo(5);
    }

    @Test
    void scrollToTopInvokesDesktopMethod() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(browser.getViewportScrollOffset()).thenReturn(new Point(0, 0));
        UUID id = register(browser);

        ScrollOffset offset = service.scroll(id, new ScrollRequest(ScrollMode.TO_TOP, null, null));
        assertThat(offset.x()).isZero();
        verify(browser).scrollToTopOfPage();
    }

    @Test
    void scrollToBottomDesktopInvokesMethod() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(browser.getViewportScrollOffset()).thenReturn(new Point(0, 100));
        UUID id = register(browser);

        service.scroll(id, new ScrollRequest(ScrollMode.TO_BOTTOM, null, null));
        verify(browser).scrollToBottomOfPage();
    }

    @Test
    void scrollToElementRequiresHandle() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        UUID id = register(browser);

        assertThatThrownBy(() -> service.scroll(id, new ScrollRequest(ScrollMode.TO_ELEMENT, null, null)))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void scrollToElementHitsRegistry() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(browser.getViewportScrollOffset()).thenReturn(new Point(0, 0));
        SessionHandle handle = SessionHandle.desktop(browser, BrowserType.CHROME, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
        registry.acquirePermit();
        registry.register(handle);
        WebElement element = mock(WebElement.class);
        String elHandle = handle.elements().put(element);

        service.scroll(handle.id(), new ScrollRequest(ScrollMode.TO_ELEMENT, elHandle, null));
        verify(browser).scrollToElement(element);
    }

    @Test
    void scrollToElementMissingHandleThrows() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        UUID id = register(browser);

        assertThatThrownBy(() -> service.scroll(id, new ScrollRequest(ScrollMode.TO_ELEMENT, "el_missing", null)))
                .isInstanceOf(ElementHandleNotFoundException.class);
    }

    @Test
    void scrollToElementCenteredDesktop() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(browser.getViewportScrollOffset()).thenReturn(new Point(0, 0));
        SessionHandle handle = SessionHandle.desktop(browser, BrowserType.CHROME, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
        registry.acquirePermit();
        registry.register(handle);
        WebElement element = mock(WebElement.class);
        String elHandle = handle.elements().put(element);

        service.scroll(handle.id(), new ScrollRequest(ScrollMode.TO_ELEMENT_CENTERED, elHandle, null));
        verify(browser).scrollToElementCentered(element);
    }

    @Test
    void scrollToElementCenteredOnMobileFallsBackToScrollToElement() {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        when(device.getViewportScrollOffset()).thenReturn(new Point(0, 0));
        SessionHandle handle = SessionHandle.mobile(device, BrowserType.ANDROID, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
        registry.acquirePermit();
        registry.register(handle);
        WebElement element = mock(WebElement.class);
        String elHandle = handle.elements().put(element);

        service.scroll(handle.id(), new ScrollRequest(ScrollMode.TO_ELEMENT_CENTERED, elHandle, null));
        verify(device).scrollToElement(element);
    }

    @Test
    void scrollToElementCenteredRequiresHandle() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        UUID id = register(browser);

        assertThatThrownBy(() -> service.scroll(id, new ScrollRequest(ScrollMode.TO_ELEMENT_CENTERED, "", null)))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void scrollDownPercentRequiresPercent() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        UUID id = register(browser);

        assertThatThrownBy(() -> service.scroll(id, new ScrollRequest(ScrollMode.DOWN_PERCENT, null, null)))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void scrollDownPercentDesktop() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(browser.getViewportScrollOffset()).thenReturn(new Point(0, 100));
        UUID id = register(browser);

        service.scroll(id, new ScrollRequest(ScrollMode.DOWN_PERCENT, null, 0.5));
        verify(browser).scrollDownPercent(0.5);
    }

    @Test
    void scrollDownFullDesktop() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(browser.getViewportScrollOffset()).thenReturn(new Point(0, 100));
        UUID id = register(browser);

        service.scroll(id, new ScrollRequest(ScrollMode.DOWN_FULL, null, null));
        verify(browser).scrollDownFull();
    }

    @Test
    void scrollToTopMobileUsesMobileMethod() {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        when(device.getViewportScrollOffset()).thenReturn(new Point(0, 0));
        SessionHandle handle = SessionHandle.mobile(device, BrowserType.ANDROID, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
        registry.acquirePermit();
        registry.register(handle);

        service.scroll(handle.id(), new ScrollRequest(ScrollMode.TO_TOP, null, null));
        verify(device).scrollToTopOfPage();
    }

    @Test
    void scrollToBottomMobileUsesMobileMethod() {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        when(device.getViewportScrollOffset()).thenReturn(new Point(0, 0));
        SessionHandle handle = SessionHandle.mobile(device, BrowserType.ANDROID, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
        registry.acquirePermit();
        registry.register(handle);

        service.scroll(handle.id(), new ScrollRequest(ScrollMode.TO_BOTTOM, null, null));
        verify(device).scrollToBottomOfPage();
    }

    @Test
    void scrollDownPercentMobile() {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        when(device.getViewportScrollOffset()).thenReturn(new Point(0, 0));
        SessionHandle handle = SessionHandle.mobile(device, BrowserType.ANDROID, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
        registry.acquirePermit();
        registry.register(handle);

        service.scroll(handle.id(), new ScrollRequest(ScrollMode.DOWN_PERCENT, null, 0.25));
        verify(device).scrollDownPercent(0.25);
    }

    @Test
    void scrollDownFullMobile() {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        when(device.getViewportScrollOffset()).thenReturn(new Point(0, 0));
        SessionHandle handle = SessionHandle.mobile(device, BrowserType.ANDROID, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
        registry.acquirePermit();
        registry.register(handle);

        service.scroll(handle.id(), new ScrollRequest(ScrollMode.DOWN_FULL, null, null));
        verify(device).scrollDownFull();
    }

    @Test
    void pageScreenshotViewportDesktop() throws Exception {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        BufferedImage img = new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB);
        when(browser.getViewportScreenshot()).thenReturn(img);
        UUID id = register(browser);

        byte[] bytes = service.pageScreenshot(id, ScreenshotStrategy.VIEWPORT);
        assertThat(bytes).isNotEmpty();
    }

    @Test
    void pageScreenshotShutterbugDesktop() throws Exception {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        when(browser.getFullPageScreenshot()).thenReturn(img);
        UUID id = register(browser);

        byte[] bytes = service.pageScreenshot(id, ScreenshotStrategy.FULL_PAGE_SHUTTERBUG);
        assertThat(bytes).isNotEmpty();
    }

    @Test
    void pageScreenshotAshotDesktop() throws Exception {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        when(browser.getFullPageScreenshotAshot()).thenReturn(img);
        UUID id = register(browser);

        byte[] bytes = service.pageScreenshot(id, ScreenshotStrategy.FULL_PAGE_ASHOT);
        assertThat(bytes).isNotEmpty();
    }

    @Test
    void pageScreenshotShutterbugPausedDesktop() throws Exception {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        when(browser.getFullPageScreenshotShutterbug()).thenReturn(img);
        UUID id = register(browser);

        byte[] bytes = service.pageScreenshot(id, ScreenshotStrategy.FULL_PAGE_SHUTTERBUG_PAUSED);
        assertThat(bytes).isNotEmpty();
    }

    @Test
    void pageScreenshotMobileViewport() throws Exception {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        when(device.getViewportScreenshot()).thenReturn(img);
        UUID id = registerMobile(device);

        byte[] bytes = service.pageScreenshot(id, ScreenshotStrategy.VIEWPORT);
        assertThat(bytes).isNotEmpty();
    }

    @Test
    void pageScreenshotMobileFullPageFallsBackToGetFullPageScreenshot() throws Exception {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        when(device.getFullPageScreenshot()).thenReturn(img);
        UUID id = registerMobile(device);

        byte[] bytes = service.pageScreenshot(id, ScreenshotStrategy.FULL_PAGE_ASHOT);
        assertThat(bytes).isNotEmpty();
        verify(device).getFullPageScreenshot();
    }

    @Test
    void pageScreenshotIoExceptionMapsToUpstream() throws Exception {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(browser.getViewportScreenshot()).thenThrow(new java.io.IOException("disk"));
        UUID id = register(browser);

        assertThatThrownBy(() -> service.pageScreenshot(id, ScreenshotStrategy.VIEWPORT))
                .isInstanceOf(io.browserservice.api.error.UpstreamUnavailableException.class);
    }

    @Test
    void removeDomOnMobileThrowsDesktopRequired() {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        UUID id = registerMobile(device);

        assertThatThrownBy(() -> service.removeDom(id, new DomRemoveRequest(DomRemovePreset.GDPR, null)))
                .isInstanceOf(DesktopSessionRequiredException.class);
    }

    @Test
    void removeDomByClassRequiresValue() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        UUID id = register(browser);

        assertThatThrownBy(() -> service.removeDom(id, new DomRemoveRequest(DomRemovePreset.BY_CLASS, null)))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void removeDomPresetsCallEngineMethods() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        UUID id = register(browser);

        service.removeDom(id, new DomRemoveRequest(DomRemovePreset.DRIFT_CHAT, null));
        verify(browser).removeDriftChat();

        service.removeDom(id, new DomRemoveRequest(DomRemovePreset.GDPR_MODAL, null));
        verify(browser).removeGDPRmodals();

        service.removeDom(id, new DomRemoveRequest(DomRemovePreset.GDPR, null));
        verify(browser).removeGDPR();

        service.removeDom(id, new DomRemoveRequest(DomRemovePreset.BY_CLASS, "chat-widget"));
        verify(browser).removeElement("chat-widget");
    }

    @Test
    void moveMouseOnMobileThrowsDesktopRequired() {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        UUID id = registerMobile(device);

        assertThatThrownBy(() -> service.moveMouse(id, new MouseMoveRequest(MouseMoveMode.OUT_OF_FRAME, null, null)))
                .isInstanceOf(DesktopSessionRequiredException.class);
    }

    @Test
    void moveMouseOutOfFrameCallsBrowser() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        UUID id = register(browser);

        service.moveMouse(id, new MouseMoveRequest(MouseMoveMode.OUT_OF_FRAME, null, null));
        verify(browser).moveMouseOutOfFrame();
    }

    @Test
    void moveMouseToNonInteractiveRequiresCoordinates() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        UUID id = register(browser);

        assertThatThrownBy(() -> service.moveMouse(id,
                new MouseMoveRequest(MouseMoveMode.TO_NON_INTERACTIVE, null, 5)))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void moveMouseToNonInteractivePassesPoint() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        UUID id = register(browser);

        service.moveMouse(id, new MouseMoveRequest(MouseMoveMode.TO_NON_INTERACTIVE, 10, 20));
        verify(browser).moveMouseToNonInteractive(new Point(10, 20));
    }

    @Test
    void executeScriptReturnsResult() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class,
                org.mockito.Mockito.withSettings().extraInterfaces(JavascriptExecutor.class));
        when(browser.getDriver()).thenReturn(driver);
        when(((JavascriptExecutor) driver).executeScript("return 42;")).thenReturn(42L);
        UUID id = register(browser);

        ExecuteResponse resp = service.executeScript(id, new ExecuteRequest("return 42;", null));
        assertThat(resp.result()).isEqualTo(42L);
    }

    @Test
    void executeScriptPassesArgs() {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class,
                org.mockito.Mockito.withSettings().extraInterfaces(JavascriptExecutor.class));
        when(browser.getDriver()).thenReturn(driver);
        UUID id = register(browser);

        service.executeScript(id, new ExecuteRequest("return arguments[0];", List.of("hello")));
        verify((JavascriptExecutor) driver).executeScript("return arguments[0];", new Object[]{"hello"});
    }

    private UUID register(Browser browser) {
        SessionHandle handle = SessionHandle.desktop(browser, BrowserType.CHROME, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
        registry.acquirePermit();
        registry.register(handle);
        return handle.id();
    }

    private UUID registerMobile(MobileDevice device) {
        SessionHandle handle = SessionHandle.mobile(device, BrowserType.ANDROID, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
        registry.acquirePermit();
        registry.register(handle);
        return handle.id();
    }

    private static EngineProperties props() {
        return new EngineProperties(
                new EngineProperties.SessionProps(10, 60, 10, 5000),
                new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000));
    }
}
