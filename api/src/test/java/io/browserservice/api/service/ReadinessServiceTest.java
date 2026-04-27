package io.browserservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.dto.ReadinessResponse;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadinessServiceTest {

  private HttpServer server;
  private int port;

  @BeforeEach
  void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/status",
        exchange -> {
          exchange.sendResponseHeaders(200, -1);
          exchange.close();
        });
    server.start();
    port = server.getAddress().getPort();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void readyWhenSeleniumReachableAndAppiumEmpty() {
    ReadinessService svc = new ReadinessService(props("http://127.0.0.1:" + port, ""));
    ReadinessResponse probe = svc.probe();
    assertThat(probe.status()).isEqualTo("ready");
    assertThat(probe.seleniumHubs())
        .singleElement()
        .satisfies(hub -> assertThat(hub.reachable()).isTrue());
    assertThat(probe.appiumServers()).isEmpty();
  }

  @Test
  void degradedWhenSeleniumUnreachable() {
    ReadinessService svc = new ReadinessService(props("http://127.0.0.1:1", ""));
    ReadinessResponse probe = svc.probe();
    assertThat(probe.status()).isEqualTo("degraded");
    assertThat(probe.seleniumHubs())
        .singleElement()
        .satisfies(hub -> assertThat(hub.reachable()).isFalse());
  }

  @Test
  void degradedWhenNoUrlsConfigured() {
    ReadinessService svc = new ReadinessService(props("", ""));
    ReadinessResponse probe = svc.probe();
    assertThat(probe.status()).isEqualTo("degraded");
    assertThat(probe.seleniumHubs()).isEmpty();
  }

  @Test
  void trailingSlashInUrlIsTolerated() {
    ReadinessService svc = new ReadinessService(props("http://127.0.0.1:" + port + "/", ""));
    assertThat(svc.probe().status()).isEqualTo("ready");
  }

  @Test
  void readyWhenEitherAppiumOrSeleniumReachableAndOtherEmpty() {
    ReadinessService svc =
        new ReadinessService(props("http://127.0.0.1:" + port, "http://127.0.0.1:" + port));
    assertThat(svc.probe().status()).isEqualTo("ready");
  }

  @Test
  void degradedWhenAppiumConfiguredButAllUnreachable() {
    ReadinessService svc =
        new ReadinessService(props("http://127.0.0.1:" + port, "http://127.0.0.1:1"));
    assertThat(svc.probe().status()).isEqualTo("degraded");
  }

  private static EngineProperties props(String selenium, String appium) {
    return new EngineProperties(
        new EngineProperties.SessionProps(10, 60, 5, 1000),
        new EngineProperties.SeleniumProps(selenium, 1000, 1000, 0, false, 0),
        new EngineProperties.AppiumProps(appium, "", "", 1000, 0),
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
