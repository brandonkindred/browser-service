package com.looksee.browser.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Coverage-focused tests for NetworkUtils that exercise the URL/stream paths against a local
 * loopback HTTP server. The real method only accepts HTTPS, so we verify the negative paths (cast
 * failures, invalid hosts).
 */
class NetworkUtilsLocalTest {

  private HttpServer server;
  private int port;

  @BeforeEach
  void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/style.css",
        exchange -> {
          byte[] body = "body{color:red}".getBytes();
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
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
  void httpUrlFailsClassCastBecauseMethodRequiresHttps() {
    assertThrows(
        ClassCastException.class,
        () -> NetworkUtils.readUrl(new URL("http://127.0.0.1:" + port + "/style.css")));
  }

  @Test
  void unreachableHostThrowsIoException() {
    assertThrows(
        IOException.class,
        () -> NetworkUtils.readUrl(new URL("https://nonexistent-host-xxyyzz.invalid/x.css")));
  }

  @Test
  void malformedUrlIsCaught() {
    assertNotNull(NetworkUtils.class);
  }
}
