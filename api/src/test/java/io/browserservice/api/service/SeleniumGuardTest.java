package io.browserservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import jakarta.inject.Inject;
import java.util.Map;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.UnreachableBrowserException;

/**
 * Exercises the CDI-managed SeleniumGuard so the SmallRye Fault Tolerance interceptors actually
 * fire — plain {@code new SeleniumGuard()} calls in the other tests deliberately bypass them.
 */
@QuarkusTest
@TestProfile(SeleniumGuardTest.NoDbProfile.class)
class SeleniumGuardTest {

  @Inject SeleniumGuard guard;

  @Inject CircuitBreakerMaintenance maintenance;

  @BeforeEach
  void resetBreaker() {
    // Tests share the application-scoped breaker; reset between cases so order doesn't matter.
    maintenance.resetAll();
  }

  @Test
  void successfulCallReturnsValueAndKeepsBreakerClosed() {
    String result = guard.execute(() -> "ok");

    assertThat(result).isEqualTo("ok");
    assertThat(maintenance.currentState("selenium")).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  void breakerOpensAfterFailuresAndRejectsSubsequentCalls() {
    // requestVolumeThreshold=20, failureRatio=0.5 → 20 consecutive failures open the breaker.
    for (int i = 0; i < 20; i++) {
      assertThatThrownBy(
              () ->
                  guard.execute(
                      () -> {
                        throw new UnreachableBrowserException("kaboom");
                      }))
          .isInstanceOf(WebDriverException.class);
    }

    assertThat(maintenance.currentState("selenium")).isEqualTo(CircuitBreakerState.OPEN);

    assertThatThrownBy(() -> guard.execute(() -> "should-not-run"))
        .isInstanceOf(CircuitBreakerOpenException.class);
  }

  public static class NoDbProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.ofEntries(
          Map.entry("quarkus.datasource.devservices.enabled", "false"),
          Map.entry("quarkus.datasource.db-kind", "h2"),
          Map.entry(
              "quarkus.datasource.jdbc.url",
              "jdbc:h2:mem:selenium-guard-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"),
          Map.entry("quarkus.datasource.username", "sa"),
          Map.entry("quarkus.datasource.password", ""),
          Map.entry("quarkus.flyway.migrate-at-start", "false"),
          Map.entry("quarkus.hibernate-orm.database.generation", "drop-and-create"));
    }
  }
}
