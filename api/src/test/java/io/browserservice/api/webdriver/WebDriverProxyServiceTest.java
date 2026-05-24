package io.browserservice.api.webdriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.error.SessionNotFoundException;
import io.browserservice.api.session.CallerId;
import java.nio.charset.StandardCharsets;
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

  @Test
  void extractSessionIdParsesW3cResponse() {
    String json =
        "{\"value\":{\"sessionId\":\"abc-123\",\"capabilities\":{\"browserName\":\"chrome\"}}}";
    String sessionId =
        WebDriverProxyService.extractSessionId(json.getBytes(StandardCharsets.UTF_8));
    assertThat(sessionId).isEqualTo("abc-123");
  }

  @Test
  void extractSessionIdReturnsNullForMalformedJson() {
    String json = "not valid json";
    String sessionId =
        WebDriverProxyService.extractSessionId(json.getBytes(StandardCharsets.UTF_8));
    assertThat(sessionId).isNull();
  }

  @Test
  void extractSessionIdReturnsNullForMissingField() {
    String json = "{\"value\":{\"capabilities\":{\"browserName\":\"chrome\"}}}";
    String sessionId =
        WebDriverProxyService.extractSessionId(json.getBytes(StandardCharsets.UTF_8));
    assertThat(sessionId).isNull();
  }

  @Test
  void extractSessionIdReturnsNullForEmptyBody() {
    assertThat(WebDriverProxyService.extractSessionId(null)).isNull();
    assertThat(WebDriverProxyService.extractSessionId(new byte[0])).isNull();
  }

  @Test
  void extractSessionIdIgnoresNestedSessionIdInCapabilities() {
    String json =
        "{\"value\":{\"sessionId\":\"real-id\","
            + "\"capabilities\":{\"sessionId\":\"fake-nested\",\"browserName\":\"chrome\"}}}";
    String sessionId =
        WebDriverProxyService.extractSessionId(json.getBytes(StandardCharsets.UTF_8));
    assertThat(sessionId).isEqualTo("real-id");
  }
}
