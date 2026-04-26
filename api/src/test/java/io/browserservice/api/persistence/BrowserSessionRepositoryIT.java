package io.browserservice.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.browserservice.api.persistence.BrowserSessionEntity.ClosedReason;
import io.browserservice.api.persistence.BrowserSessionEntity.Status;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class BrowserSessionRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("browser_service")
                    .withUsername("browser_service")
                    .withPassword("browser_service");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private BrowserSessionRepository repository;

    @Test
    void insertAndFindActiveSession() {
        BrowserSessionEntity entity = newActive();

        repository.saveAndFlush(entity);
        repository.findById(entity.getId()).ifPresentOrElse(found -> {
            assertThat(found.getStatus()).isEqualTo(Status.ACTIVE);
            assertThat(found.getBrowserType()).isEqualTo("CHROME");
            assertThat(found.getEnvironment()).isEqualTo("TEST");
            assertThat(found.isMobile()).isFalse();
            assertThat(found.getIdleTtlSecs()).isEqualTo(300);
            assertThat(found.getAbsoluteTtlSecs()).isEqualTo(1800);
        }, () -> {
            throw new AssertionError("expected entity not found");
        });
    }

    @Test
    void updateToClosedRecordsReason() {
        BrowserSessionEntity entity = repository.saveAndFlush(newActive());

        entity.setStatus(Status.CLOSED);
        entity.setClosedReason(ClosedReason.CLIENT);
        entity.setClosedAt(Instant.now());
        repository.saveAndFlush(entity);

        BrowserSessionEntity found = repository.findById(entity.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(Status.CLOSED);
        assertThat(found.getClosedReason()).isEqualTo(ClosedReason.CLIENT);
        assertThat(found.getClosedAt()).isNotNull();
    }

    @Test
    void findByStatusReturnsOnlyMatching() {
        BrowserSessionEntity active = repository.saveAndFlush(newActive());
        BrowserSessionEntity closed = newActive();
        closed.setStatus(Status.CLOSED);
        closed.setClosedReason(ClosedReason.CLIENT);
        closed.setClosedAt(Instant.now());
        repository.saveAndFlush(closed);

        List<BrowserSessionEntity> activeRows = repository.findByStatus(Status.ACTIVE);
        assertThat(activeRows).extracting(BrowserSessionEntity::getId).contains(active.getId());
        assertThat(activeRows).extracting(BrowserSessionEntity::getId).doesNotContain(closed.getId());
    }

    @Test
    void expiredReapWritesReapedReason() {
        BrowserSessionEntity entity = repository.saveAndFlush(newActive());

        entity.setStatus(Status.EXPIRED);
        entity.setClosedReason(ClosedReason.REAPED_ABSOLUTE);
        entity.setClosedAt(Instant.now());
        repository.saveAndFlush(entity);

        BrowserSessionEntity found = repository.findById(entity.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(Status.EXPIRED);
        assertThat(found.getClosedReason()).isEqualTo(ClosedReason.REAPED_ABSOLUTE);
    }

    private static BrowserSessionEntity newActive() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new BrowserSessionEntity(
                UUID.randomUUID(),
                "CHROME",
                "TEST",
                Status.ACTIVE,
                false,
                now,
                now,
                now.plusSeconds(300),
                300,
                1800);
    }
}
