package io.browserservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.looksee.browser.Browser;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.dto.CaptureRequest;
import io.browserservice.api.dto.PngEncoding;
import io.browserservice.api.error.UpstreamUnavailableException;
import io.browserservice.api.session.CaptureScreenshotCache;
import io.browserservice.api.session.DriverFactory;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

class CaptureServiceMoreTest {

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
  void captureRuntimeFromNavigatePropagatesAfterCleanup() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class);
    when(browser.getDriver()).thenReturn(driver);
    doThrow(new RuntimeException("boom")).when(browser).navigateTo("u");
    when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

    assertThatThrownBy(
            () ->
                service.capture(
                    new CaptureRequest("u", BrowserType.CHROME, null, null, null, null, null)))
        .isInstanceOf(RuntimeException.class);

    // Permit released after the capture session was cleaned up.
    assertThat(registry.size()).isZero();
    assertThat(registry.availablePermits()).isEqualTo(5);
  }

  @Test
  void captureIoFromScreenshotMapsToUpstreamUnavailable() throws Exception {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class);
    when(browser.getDriver()).thenReturn(driver);
    when(driver.getCurrentUrl()).thenReturn("u");
    when(browser.getViewportScreenshot()).thenThrow(new java.io.IOException("disk"));
    when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

    assertThatThrownBy(
            () ->
                service.capture(
                    new CaptureRequest(
                        "u", BrowserType.CHROME, null, null, PngEncoding.BASE64, null, null)))
        .isInstanceOf(UpstreamUnavailableException.class);
    assertThat(registry.size()).isZero();
  }

  @Test
  void captureWithEmptyXpathSkipsElementLookup() throws Exception {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class);
    when(browser.getDriver()).thenReturn(driver);
    when(driver.getCurrentUrl()).thenReturn("u");
    when(browser.getViewportScreenshot())
        .thenReturn(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
    when(drivers.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST)).thenReturn(browser);

    var resp =
        service.capture(
            new CaptureRequest("u", BrowserType.CHROME, null, null, PngEncoding.BASE64, "", null));
    assertThat(resp.element()).isNull();
  }

  private static EngineProperties props() {
    return new EngineProperties(
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
            16777216));
  }
}
