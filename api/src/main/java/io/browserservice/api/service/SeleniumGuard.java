package io.browserservice.api.service;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * Single shared fault-tolerance guard around all WebDriver-touching operations on this replica.
 *
 * <p>Provides one circuit breaker, one bulkhead, and a per-call timeout. Retry is intentionally NOT
 * here — idempotency is a per-operation policy that callers declare via {@code @Retry} on the
 * public service method.
 */
@ApplicationScoped
public class SeleniumGuard {

  /**
   * Runs {@code op} under the shared circuit breaker, bulkhead, and 5s timeout. The timeout doubles
   * as the "slow-call" detector the issue calls for: any call exceeding 5s throws {@link
   * org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException}, which counts as a failure
   * against the breaker's {@code failureRatio}.
   */
  @CircuitBreaker(
      requestVolumeThreshold = 20,
      failureRatio = 0.5,
      delay = 30,
      delayUnit = ChronoUnit.SECONDS,
      successThreshold = 2)
  @CircuitBreakerName("selenium")
  @Bulkhead(4)
  @Timeout(value = 5, unit = ChronoUnit.SECONDS)
  public <T> T execute(Supplier<T> op) {
    return op.get();
  }
}
