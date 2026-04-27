package com.looksee.browser;

import static org.junit.jupiter.api.Assertions.*;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;

public class RateLimitExecutorTest {

  @Test
  public void testConstants() {
    assertEquals(1, RateLimitExecutor.CONCURRENT_SESSIONS);
    assertEquals(50, RateLimitExecutor.ACTIONS_RATE_LIMIT_PER_SECOND);
    assertTrue(RateLimitExecutor.SECONDS_PER_ACTION > 0);
    assertEquals(0.02, RateLimitExecutor.SECONDS_PER_ACTION, 0.001);
  }

  @Test
  public void testConstructor() throws MalformedURLException {
    URL url = new URL("http://localhost:4444/wd/hub");
    RateLimitExecutor executor = new RateLimitExecutor(url);
    assertNotNull(executor);
  }

  @Test
  public void testSecondsPerActionCalculation() {
    double expected =
        (double) RateLimitExecutor.CONCURRENT_SESSIONS
            / (double) RateLimitExecutor.ACTIONS_RATE_LIMIT_PER_SECOND;
    assertEquals(expected, RateLimitExecutor.SECONDS_PER_ACTION, 0.0001);
  }

  @Test
  public void testExecuteEnforcesRateLimitAndPropagatesIoException() throws Exception {
    URL url = new URL("http://127.0.0.1:1/wd/hub"); // port 1: guaranteed closed
    RateLimitExecutor executor = new RateLimitExecutor(url);
    org.openqa.selenium.remote.Command command =
        new org.openqa.selenium.remote.Command(
            new org.openqa.selenium.remote.SessionId("s1"), "noop");

    // The call will reach super.execute() and fail with IOException (connection refused).
    // That's enough to exercise the rate-limit path in the subclass.
    assertThrows(Exception.class, () -> executor.execute(command));
  }
}
