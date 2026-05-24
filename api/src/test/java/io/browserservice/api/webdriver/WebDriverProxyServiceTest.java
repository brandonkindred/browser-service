package io.browserservice.api.webdriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.error.SessionNotFoundException;
import io.browserservice.api.session.CallerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebDriverProxyServiceTest {

  private static final CallerId ALICE = CallerId.of("test-tenant", "alice");
  private static final CallerId BOB = CallerId.of("test-tenant", "bob");

  private WebDriverProxyService service;

  @BeforeEach
  void setUp() {
    EngineProperties props =
        new EngineProperties(
            new EngineProperties.SessionProps(10, 60, 2, 1000),
            new EngineProperties.SeleniumProps(
                "http://localhost:4444/wd/hub", 5000, 60000, 3, false, 10),
            new EngineProperties.AppiumProps("", "", "", 0, 0),
            new EngineProperties.BrowserStackProps(
                false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
            new EngineProperties.WebSocketProps(
                32, 300, 64, 10000, true, 250, true, 1000, true, 2000, 50, 16777216),
            new EngineProperties.SecurityProps(java.util.List.of()));
    service = new WebDriverProxyService(props);
  }

  @Test
  void forwardRequiresOwnership() {
    assertThatThrownBy(() -> service.forward("GET", "nonexistent", "url", null, ALICE))
        .isInstanceOf(SessionNotFoundException.class);
  }

  @Test
  void forwardRejectsUnknownSession() {
    assertThatThrownBy(() -> service.forward("GET", "fake-session-id", "url", null, BOB))
        .isInstanceOf(SessionNotFoundException.class);
  }

  @Test
  void findSessionReturnsEmptyForUnknown() {
    assertThat(service.findSession("nonexistent")).isEmpty();
  }

  @Test
  void removeSessionCleansUp() {
    service.removeSession("some-id");
    assertThat(service.findSession("some-id")).isEmpty();
  }
}
