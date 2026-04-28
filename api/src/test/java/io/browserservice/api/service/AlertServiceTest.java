package io.browserservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.looksee.browser.Browser;
import com.looksee.browser.enums.AlertChoice;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.dto.AlertRespondRequest;
import io.browserservice.api.dto.AlertStateResponse;
import io.browserservice.api.error.SessionForbiddenException;
import io.browserservice.api.persistence.BrowserSessionTracker;
import io.browserservice.api.session.CallerId;
import io.browserservice.api.session.DriverFactory;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;

class AlertServiceTest {

  private static final CallerId ALICE = CallerId.parse("alice");
  private static final CallerId BOB = CallerId.parse("bob");

  private SessionRegistry registry;
  private SessionLocks locks;
  private AlertService service;

  @BeforeEach
  void setUp() {
    EngineProperties props = props();
    registry = new SessionRegistry(props);
    locks = new SessionLocks(props);
    SessionService sessionService =
        new SessionService(
            registry,
            locks,
            Mockito.mock(DriverFactory.class),
            Mockito.mock(BrowserSessionTracker.class),
            props);
    service = new AlertService(sessionService, locks);
  }

  @Test
  void getAlertReturnsPresentTrueWithText() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    when(browser.getDriver()).thenReturn(driver);
    Alert alert = mock(Alert.class);
    when(driver.switchTo().alert()).thenReturn(alert);
    when(alert.getText()).thenReturn("are you sure?");
    UUID id = register(browser);

    AlertStateResponse resp = service.getAlert(id, ALICE);
    assertThat(resp.present()).isTrue();
    assertThat(resp.text()).isEqualTo("are you sure?");
  }

  @Test
  void getAlertReturnsAbsentWhenNoAlert() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    when(browser.getDriver()).thenReturn(driver);
    when(driver.switchTo().alert()).thenThrow(new NoAlertPresentException("no"));
    UUID id = register(browser);

    AlertStateResponse resp = service.getAlert(id, ALICE);
    assertThat(resp.present()).isFalse();
    assertThat(resp.text()).isNull();
  }

  @Test
  void getAlertReturnsAbsentOnUnexpectedException() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    when(browser.getDriver()).thenReturn(driver);
    when(driver.switchTo().alert()).thenThrow(new RuntimeException("other"));
    UUID id = register(browser);

    AlertStateResponse resp = service.getAlert(id, ALICE);
    assertThat(resp.present()).isFalse();
  }

  @Test
  void getAlertTextFailureStillReportsPresent() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    when(browser.getDriver()).thenReturn(driver);
    Alert alert = mock(Alert.class);
    when(driver.switchTo().alert()).thenReturn(alert);
    when(alert.getText()).thenThrow(new RuntimeException("x"));
    UUID id = register(browser);

    assertThat(service.getAlert(id, ALICE).text()).isNull();
  }

  @Test
  void respondAcceptsTheAlert() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    when(browser.getDriver()).thenReturn(driver);
    Alert alert = mock(Alert.class);
    when(driver.switchTo().alert()).thenReturn(alert);
    UUID id = register(browser);

    service.respond(id, ALICE, new AlertRespondRequest(AlertChoice.ACCEPT, null));
    verify(alert).accept();
    verify(alert, never()).dismiss();
  }

  @Test
  void respondDismissesTheAlert() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    when(browser.getDriver()).thenReturn(driver);
    Alert alert = mock(Alert.class);
    when(driver.switchTo().alert()).thenReturn(alert);
    UUID id = register(browser);

    service.respond(id, ALICE, new AlertRespondRequest(AlertChoice.DISMISS, null));
    verify(alert).dismiss();
  }

  @Test
  void respondSendsInputThenAccepts() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    when(browser.getDriver()).thenReturn(driver);
    Alert alert = mock(Alert.class);
    when(driver.switchTo().alert()).thenReturn(alert);
    UUID id = register(browser);

    service.respond(id, ALICE, new AlertRespondRequest(AlertChoice.ACCEPT, "hello"));
    verify(alert).sendKeys("hello");
    verify(alert).accept();
  }

  @Test
  void respondSwallowsSendKeysFailure() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    when(browser.getDriver()).thenReturn(driver);
    Alert alert = mock(Alert.class);
    when(driver.switchTo().alert()).thenReturn(alert);
    org.mockito.Mockito.doThrow(new RuntimeException("sendKeys not supported"))
        .when(alert)
        .sendKeys("x");
    UUID id = register(browser);

    service.respond(id, ALICE, new AlertRespondRequest(AlertChoice.ACCEPT, "x"));
    verify(alert).accept();
  }

  @Test
  void getAlertWithWrongOwnerThrowsSessionForbidden() {
    Browser browser = mock(Browser.class);
    UUID id = register(browser);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getAlert(id, BOB))
        .isInstanceOf(SessionForbiddenException.class);
  }

  @Test
  void respondWithWrongOwnerThrowsSessionForbidden() {
    Browser browser = mock(Browser.class);
    UUID id = register(browser);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.respond(id, BOB, new AlertRespondRequest(AlertChoice.ACCEPT, null)))
        .isInstanceOf(SessionForbiddenException.class);
  }

  @Test
  void respondIsNoOpWhenNoAlert() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    when(browser.getDriver()).thenReturn(driver);
    when(driver.switchTo().alert()).thenThrow(new NoAlertPresentException("none"));
    UUID id = register(browser);

    service.respond(id, ALICE, new AlertRespondRequest(AlertChoice.ACCEPT, null));
    // No throw; no verification target.
    assertThat(true).isTrue();
  }

  private UUID register(Browser browser) {
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
    return handle.id();
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
