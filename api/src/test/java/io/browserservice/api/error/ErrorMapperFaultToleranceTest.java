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
  void bulkheadMapsTo503WithRetryAfterDetail() {
    ErrorMapper.Mapped mapped = ErrorMapper.map(new BulkheadException("at-capacity"), "req-2");

    assertThat(mapped.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(mapped.body().code()).isEqualTo("concurrency_exceeded");
    assertThat(mapped.body().details()).containsEntry(ErrorMapper.RETRY_AFTER_KEY, 1);
  }

  @Test
  void faultToleranceTimeoutMapsTo504() {
    ErrorMapper.Mapped mapped = ErrorMapper.map(new TimeoutException("slow upstream"), "req-3");

    assertThat(mapped.status()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    assertThat(mapped.body().code()).isEqualTo("upstream_timeout");
    // Timeout has no Retry-After hint — caller decides whether to retry; we don't promise recovery.
    assertThat(mapped.body().details()).isNull();
  }
}
