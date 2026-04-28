package io.browserservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.looksee.browser.Browser;
import com.looksee.browser.MobileDevice;
import com.looksee.browser.enums.Action;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import com.looksee.browser.enums.MobileAction;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.dto.ElementActionRequest;
import io.browserservice.api.dto.ElementScreenshotRequest;
import io.browserservice.api.dto.ElementStateResponse;
import io.browserservice.api.dto.ElementTouchRequest;
import io.browserservice.api.dto.FindElementRequest;
import io.browserservice.api.error.DesktopSessionRequiredException;
import io.browserservice.api.error.MobileSessionRequiredException;
import io.browserservice.api.error.SessionForbiddenException;
import io.browserservice.api.persistence.BrowserSessionTracker;
import io.browserservice.api.session.CallerId;
import io.browserservice.api.session.DriverFactory;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

class ElementOperationsServiceTest {

  private static final CallerId ALICE = CallerId.parse("alice");
  private static final CallerId BOB = CallerId.parse("bob");

  private SessionRegistry registry;
  private SessionLocks locks;
  private ElementOperationsService service;

  @BeforeEach
  void setUp() {
    EngineProperties props = props();
    registry = new SessionRegistry(props);
    locks = new SessionLocks(props);
    SessionService sessionService =
        new SessionService(
            registry,
            locks,
            org.mockito.Mockito.mock(DriverFactory.class),
            org.mockito.Mockito.mock(BrowserSessionTracker.class),
            props);
    service = new ElementOperationsService(sessionService, locks);
  }

  @Test
  void findDesktopRegistersHandleAndReturnsState() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class);
    when(browser.getDriver()).thenReturn(driver);
    WebElement element = mock(WebElement.class);
    when(browser.findElement("//h1")).thenReturn(element);
    when(element.isDisplayed()).thenReturn(true);
    when(element.getRect()).thenReturn(new Rectangle(1, 2, 3, 4));
    when(browser.extractAttributes(element)).thenReturn(Map.of("id", "x"));
    SessionHandle handle = registerDesktop(browser);

    ElementStateResponse resp = service.find(handle.id(), ALICE, new FindElementRequest("//h1"));

    assertThat(resp.found()).isTrue();
    assertThat(resp.displayed()).isTrue();
    assertThat(resp.attributes()).containsEntry("id", "x");
    assertThat(resp.rect().x()).isEqualTo(1);
    assertThat(resp.elementHandle()).isEqualTo("el_1");
    assertThat(handle.elements().get(resp.elementHandle())).isSameAs(element);
  }

  @Test
  void findMobileDelegatesToDevice() {
    MobileDevice device = mock(MobileDevice.class);
    WebDriver driver = mock(WebDriver.class);
    when(device.getDriver()).thenReturn(driver);
    WebElement element = mock(WebElement.class);
    when(device.findElement("//btn")).thenReturn(element);
    when(device.extractAttributes(element)).thenReturn(Map.of());
    when(element.isDisplayed()).thenReturn(false);
    when(element.getRect()).thenThrow(new RuntimeException("nope"));
    SessionHandle handle = registerMobile(device);

    ElementStateResponse resp = service.find(handle.id(), ALICE, new FindElementRequest("//btn"));

    assertThat(resp.found()).isTrue();
    assertThat(resp.displayed()).isFalse();
    assertThat(resp.rect()).isNull();
  }

  @Test
  void findReturnsAbsentStateWhenMissing() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class);
    when(browser.getDriver()).thenReturn(driver);
    when(browser.findElement("//missing")).thenThrow(new NoSuchElementException("no"));
    SessionHandle handle = registerDesktop(browser);

    ElementStateResponse resp =
        service.find(handle.id(), ALICE, new FindElementRequest("//missing"));
    assertThat(resp.found()).isFalse();
    assertThat(resp.elementHandle()).isNull();
    assertThat(resp.attributes()).isEmpty();
  }

  @Test
  void findHandlesDisplayExceptionAsFalse() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class);
    when(browser.getDriver()).thenReturn(driver);
    WebElement element = mock(WebElement.class);
    when(browser.findElement("//x")).thenReturn(element);
    when(element.isDisplayed()).thenThrow(new RuntimeException("boom"));
    when(browser.extractAttributes(element)).thenReturn(Map.of());
    SessionHandle handle = registerDesktop(browser);

    ElementStateResponse resp = service.find(handle.id(), ALICE, new FindElementRequest("//x"));
    assertThat(resp.displayed()).isFalse();
  }

  @Test
  void actionOnMobileThrowsDesktopRequired() {
    MobileDevice device = mock(MobileDevice.class);
    WebDriver driver = mock(WebDriver.class);
    when(device.getDriver()).thenReturn(driver);
    SessionHandle handle = registerMobile(device);

    assertThatThrownBy(
            () ->
                service.action(
                    handle.id(), ALICE, new ElementActionRequest("el_1", Action.CLICK, null)))
        .isInstanceOf(DesktopSessionRequiredException.class);
  }

  @Test
  void actionDesktopResolvesHandleBeforeInvokingFactory() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class);
    when(browser.getDriver()).thenReturn(driver);
    SessionHandle handle = registerDesktop(browser);

    // Missing handle is the deterministic way to prove the desktop routing + handle-resolution path
    // is reached (without exercising the untestable ActionFactory internals).
    assertThatThrownBy(
            () ->
                service.action(
                    handle.id(), ALICE, new ElementActionRequest("el_missing", Action.CLICK, null)))
        .isInstanceOf(io.browserservice.api.error.ElementHandleNotFoundException.class);
  }

  @Test
  void touchOnDesktopThrowsMobileRequired() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class);
    when(browser.getDriver()).thenReturn(driver);
    SessionHandle handle = registerDesktop(browser);

    assertThatThrownBy(
            () ->
                service.touch(
                    handle.id(), ALICE, new ElementTouchRequest("el_1", MobileAction.TAP, null)))
        .isInstanceOf(MobileSessionRequiredException.class);
  }

  @Test
  void elementScreenshotDesktopEncodesPng() throws Exception {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class);
    when(browser.getDriver()).thenReturn(driver);
    WebElement element = mock(WebElement.class);
    when(browser.getElementScreenshot(element))
        .thenReturn(new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB));
    SessionHandle handle = registerDesktop(browser);
    String h = handle.elements().put(element);

    byte[] bytes =
        service.elementScreenshot(handle.id(), ALICE, new ElementScreenshotRequest(h, null));
    assertThat(bytes).isNotEmpty();
  }

  @Test
  void elementScreenshotMobileEncodesPng() throws Exception {
    MobileDevice device = mock(MobileDevice.class);
    WebDriver driver = mock(WebDriver.class);
    when(device.getDriver()).thenReturn(driver);
    WebElement element = mock(WebElement.class);
    when(device.getElementScreenshot(element))
        .thenReturn(new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB));
    SessionHandle handle = registerMobile(device);
    String h = handle.elements().put(element);

    byte[] bytes =
        service.elementScreenshot(handle.id(), ALICE, new ElementScreenshotRequest(h, null));
    assertThat(bytes).isNotEmpty();
  }

  @Test
  void elementScreenshotDesktopFailureMapsToUpstream() throws Exception {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class);
    when(browser.getDriver()).thenReturn(driver);
    WebElement element = mock(WebElement.class);
    when(browser.getElementScreenshot(element)).thenThrow(new java.io.IOException("bad"));
    SessionHandle handle = registerDesktop(browser);
    String h = handle.elements().put(element);

    assertThatThrownBy(
            () ->
                service.elementScreenshot(
                    handle.id(), ALICE, new ElementScreenshotRequest(h, null)))
        .isInstanceOf(io.browserservice.api.error.UpstreamUnavailableException.class);
  }

  @Test
  void elementScreenshotRuntimePropagates() throws Exception {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class);
    when(browser.getDriver()).thenReturn(driver);
    WebElement element = mock(WebElement.class);
    when(browser.getElementScreenshot(element)).thenThrow(new RuntimeException("engine"));
    SessionHandle handle = registerDesktop(browser);
    String h = handle.elements().put(element);

    assertThatThrownBy(
            () ->
                service.elementScreenshot(
                    handle.id(), ALICE, new ElementScreenshotRequest(h, null)))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void findWithWrongOwnerThrowsSessionForbidden() {
    Browser browser = mock(Browser.class);
    SessionHandle handle = registerDesktop(browser);
    assertThatThrownBy(() -> service.find(handle.id(), BOB, new FindElementRequest("//x")))
        .isInstanceOf(SessionForbiddenException.class);
  }

  private SessionHandle registerDesktop(Browser browser) {
    SessionHandle handle =
        SessionHandle.desktop(
            browser,
            ALICE,
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60));
    registry.acquirePermit();
    registry.register(handle);
    return handle;
  }

  private SessionHandle registerMobile(MobileDevice device) {
    SessionHandle handle =
        SessionHandle.mobile(
            device,
            ALICE,
            BrowserType.ANDROID,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60));
    registry.acquirePermit();
    registry.register(handle);
    return handle;
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
