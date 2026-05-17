package io.browserservice.api.service;

import io.browserservice.api.error.UpstreamUnavailableException;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriverException;

/**
 * Shared fault-tolerance guard around the WebDriver-touching operations exposed by {@link
 * BrowserOperationsService} and {@link ElementOperationsService}.
 *
 * <p>Provides one circuit breaker and one bulkhead. Retry and timeout are intentionally NOT here —
 * they're per-operation policies: idempotency varies, and a one-size-fits-all timeout would kill
 * legitimate slow operations (navigation, full-page screenshots, user JS). Callers add
 * {@code @Retry} and {@code @Timeout} on the public service method when appropriate.
 *
 * <p><strong>Coverage gap:</strong> several other WebDriver call sites (notably {@code
 * CaptureService}, {@code AlertService}, {@code SessionService.describe}, and the WS watchers)
 * currently bypass this guard. Widening coverage is tracked separately; treat the breaker /
 * bulkhead as protecting BrowserOperations / ElementOperations only.
 *
 * <p><strong>Known limitation:</strong> SmallRye {@code @Timeout} uses {@code Thread.interrupt()}
 * on the worker thread. Selenium's HTTP client doesn't respond to interrupts, so when a caller's
 * {@code @Timeout} fires, the underlying Selenium call keeps running until its own HTTP read
 * timeout (60s by default; see {@code browserservice.selenium.read-timeout-ms}). During that gap
 * the bulkhead permit and per-session lock are still held by the zombie thread. Subsequent requests
 * to the same session can see {@code SessionBusyException}, and four concurrent zombies fully
 * saturate the bulkhead. Mitigations would require moving the guard to {@code @Asynchronous} (with
 * the cost of ThreadLocal/context-propagation work) or lowering the Selenium HTTP read timeout
 * (with the cost of killing legitimate slow operations).
 */
@ApplicationScoped
public class SeleniumGuard {

  /** Runs {@code op} under the shared circuit breaker and bulkhead. */
  @CircuitBreaker(
      requestVolumeThreshold = 20,
      failureRatio = 0.5,
      delay = 30,
      delayUnit = ChronoUnit.SECONDS,
      successThreshold = 2,
      // Only genuine upstream Selenium failures count toward the breaker. failOn is the positive
      // whitelist; client-side ApiExceptions (SessionBusy, ElementHandleNotFound, Validation, …)
      // and BulkheadException simply don't appear in it and pass through without affecting state.
      // skipOn carves out WebDriverException subclasses that represent page-state / user errors
      // (per ErrorMapper's 4xx taxonomy) — these inherit from WebDriverException but a chatty
      // page with a stuck alert or stale elements should not take the replica offline.
      failOn = {WebDriverException.class, UpstreamUnavailableException.class},
      skipOn = {
        NoSuchElementException.class,
        TimeoutException.class,
        UnhandledAlertException.class,
        StaleElementReferenceException.class
      })
  @CircuitBreakerName("selenium")
  @Bulkhead(4)
  public <T> T execute(Supplier<T> op) {
    return op.get();
  }
}
