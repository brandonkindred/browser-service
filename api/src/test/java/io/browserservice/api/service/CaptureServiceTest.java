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
import io.browserservice.api.dto.CaptureRequest;
import io.browserservice.api.dto.CaptureResponse;
import io.browserservice.api.dto.PngEncoding;
import io.browserservice.api.dto.ScreenshotStrategy;
import io.browserservice.api.error.UpstreamUnavailableException;
import io.browserservice.api.session.CaptureScreenshotCache;
import io.browserservice.api.session.DriverFactory;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import java.awt.image.BufferedImage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

class CaptureServiceTest {

    private SessionRegistry registry;
    private SessionLocks locks;
    private DriverFactory drivers;
    private CaptureScreenshotCache cache;
    private CaptureService service;

    @BeforeEach
    void setUp() {
        EngineProperties props = props();
        registry = new SessionRegistry(props);
        locks = new SessionLocks(props);
        drivers = mock(DriverFactory.class);
        cache = new CaptureScreenshotCache(props);
        service = new CaptureService(registry, locks, drivers, cache, props);
    }

    @Test
    void captureDesktopInlineBase64() throws Exception {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("https://example.com");
        when(browser.getViewportScreenshot()).thenReturn(new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB));
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        CaptureResponse resp = service.capture(new CaptureRequest("https://example.com",
                BrowserType.CHROME, null, ScreenshotStrategy.VIEWPORT, PngEncoding.BASE64, null, null));

        assertThat(resp.screenshot().imageBase64()).isNotBlank();
        assertThat(resp.screenshot().href()).isNull();
        assertThat(resp.currentUrl()).isEqualTo("https://example.com");
        assertThat(registry.size()).isZero(); // session closed after capture
    }

    @Test
    void captureDesktopBinaryReturnsHref() throws Exception {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("u");
        when(browser.getViewportScreenshot()).thenReturn(new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB));
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        CaptureResponse resp = service.capture(new CaptureRequest("u",
                BrowserType.CHROME, null, null, null, null, null));

        assertThat(resp.screenshot().href()).startsWith("/v1/capture/");
        assertThat(resp.screenshot().imageBase64()).isNull();
    }

    @Test
    void captureIncludesSourceWhenRequested() throws Exception {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("u");
        when(browser.getViewportScreenshot()).thenReturn(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        when(browser.getSource()).thenReturn("<html>hi</html>");
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        CaptureResponse resp = service.capture(new CaptureRequest("u",
                BrowserType.CHROME, null, null, PngEncoding.BASE64, null, true));

        assertThat(resp.source()).isEqualTo("<html>hi</html>");
    }

    @Test
    void captureWithXpathResolvesElement() throws Exception {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("u");
        when(browser.getViewportScreenshot()).thenReturn(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        WebElement element = mock(WebElement.class);
        when(browser.findElement("//h1")).thenReturn(element);
        when(browser.extractAttributes(element)).thenReturn(Map.of("id", "x"));
        when(element.isDisplayed()).thenReturn(true);
        when(element.getRect()).thenReturn(new Rectangle(1, 2, 3, 4));
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        CaptureResponse resp = service.capture(new CaptureRequest("u",
                BrowserType.CHROME, null, null, PngEncoding.BASE64, "//h1", null));

        assertThat(resp.element()).isNotNull();
        assertThat(resp.element().found()).isTrue();
        assertThat(resp.element().attributes()).containsEntry("id", "x");
    }

    @Test
    void captureWithXpathMissingElementReturnsAbsentElementState() throws Exception {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("u");
        when(browser.getViewportScreenshot()).thenReturn(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        when(browser.findElement("//missing")).thenThrow(new NoSuchElementException("nope"));
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        CaptureResponse resp = service.capture(new CaptureRequest("u",
                BrowserType.CHROME, null, null, PngEncoding.BASE64, "//missing", null));

        assertThat(resp.element().found()).isFalse();
    }

    @Test
    void captureMobileUsesMobileDevice() throws Exception {
        MobileDevice device = mock(MobileDevice.class);
        WebDriver driver = mock(WebDriver.class);
        when(device.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("u");
        when(device.getViewportScreenshot()).thenReturn(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        when(drivers.createMobile(BrowserType.ANDROID, BrowserEnvironment.TEST)).thenReturn(device);

        CaptureResponse resp = service.capture(new CaptureRequest("u",
                BrowserType.ANDROID, null, null, PngEncoding.BASE64, null, null));
        assertThat(resp).isNotNull();
        verify(device).navigateTo("u");
    }

    @Test
    void captureDriverFailureReleasesPermit() {
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST))
                .thenThrow(new UpstreamUnavailableException("nope"));

        assertThatThrownBy(() -> service.capture(new CaptureRequest("u",
                BrowserType.CHROME, null, null, null, null, null)))
                .isInstanceOf(UpstreamUnavailableException.class);
        assertThat(registry.availablePermits()).isEqualTo(5);
    }

    @Test
    void fetchScreenshotReadsFromCache() throws Exception {
        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("u");
        when(browser.getViewportScreenshot()).thenReturn(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

        CaptureResponse resp = service.capture(new CaptureRequest("u",
                BrowserType.CHROME, null, null, null, null, null));
        java.util.UUID id = java.util.UUID.fromString(resp.screenshot().href().substring("/v1/capture/".length(),
                resp.screenshot().href().length() - "/screenshot".length()));

        CaptureScreenshotCache.CaptureEntry entry = service.fetchScreenshot(id);
        assertThat(entry.pngBytes()).isNotEmpty();
    }

    private static EngineProperties props() {
        return new EngineProperties(
                new EngineProperties.SessionProps(10, 60, 5, 5000),
                new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000, true, 250, true, 1000, true, 2000, 50, 16777216));
    }
}
