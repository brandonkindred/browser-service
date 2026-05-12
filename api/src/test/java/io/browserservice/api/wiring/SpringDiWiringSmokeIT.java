package io.browserservice.api.wiring;

import static org.assertj.core.api.Assertions.assertThat;

import com.looksee.browser.config.SeleniumProperties;
import io.browserservice.api.service.SessionService;
import io.browserservice.api.session.SessionRegistry;
import io.browserservice.api.ws.push.WatcherCoordinator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(SpringDiWiringSmokeIT.WiringProfile.class)
class SpringDiWiringSmokeIT {

  @Inject SessionService sessionService;

  @Inject SessionRegistry sessionRegistry;

  @Inject SeleniumProperties seleniumProperties;

  @Inject
  @Named("webSocketScheduler")
  ScheduledExecutorService webSocketScheduler;

  @Inject WatcherCoordinator watcherCoordinator;

  @Test
  void allBeansResolved() {
    assertThat(sessionService).isNotNull();
    assertThat(sessionRegistry).isNotNull();
    assertThat(seleniumProperties).isNotNull();
    assertThat(webSocketScheduler).isNotNull();
    assertThat(watcherCoordinator).isNotNull();
  }

  public static class WiringProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.ofEntries(
          Map.entry("quarkus.datasource.devservices.enabled", "false"),
          Map.entry("quarkus.datasource.db-kind", "h2"),
          Map.entry(
              "quarkus.datasource.jdbc.url",
              "jdbc:h2:mem:wiring-smoke;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"),
          Map.entry("quarkus.datasource.username", "sa"),
          Map.entry("quarkus.datasource.password", ""),
          Map.entry("quarkus.flyway.migrate-at-start", "false"),
          Map.entry("quarkus.hibernate-orm.database.generation", "drop-and-create"),
          Map.entry("browserservice.selenium.urls", "http://localhost:4444/wd/hub"),
          Map.entry("quarkus.http.test-port", "0"));
    }
  }
}
