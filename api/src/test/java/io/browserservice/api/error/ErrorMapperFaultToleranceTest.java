package io.browserservice.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorMapperFaultToleranceTest {

  @Test
  void circuitBreakerOpenMapsTo503WithRetryAfterDetail() {
    ErrorMapper.Mapped mapped =
        ErrorMapper.map(new CircuitBreakerOpenException("selenium open"), "req-1");

    assertThat(mapped.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(mapped.body().code()).isEqualTo("selenium_circuit_open");
    assertThat(mapped.body().details()).containsEntry(ErrorMapper.RETRY_AFTER_KEY, 30);
    assertThat(mapped.body().requestId()).isEqualTo("req-1");
  }

  @Test
  void bulkheadMapsTo503WithoutRetryAfter() {
    // No Retry-After: holders are bounded only by per-method @Timeout (some methods have none),
    // so any specific hint would lie to clients. Backoff is the caller's responsibility.
    ErrorMapper.Mapped mapped = ErrorMapper.map(new BulkheadException("at-capacity"), "req-2");

    assertThat(mapped.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(mapped.body().code()).isEqualTo("concurrency_exceeded");
    assertThat(mapped.body().details()).isNull();
  }

  @Test
  void faultToleranceTimeoutMapsTo504WithDistinctCode() {
    // Distinct code from the Selenium TimeoutException → 408 upstream_timeout case so callers
    // parsing the error code can tell them apart.
    ErrorMapper.Mapped mapped = ErrorMapper.map(new TimeoutException("slow upstream"), "req-3");

    assertThat(mapped.status()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    assertThat(mapped.body().code()).isEqualTo("selenium_call_timeout");
    assertThat(mapped.body().details()).isNull();
  }
}
