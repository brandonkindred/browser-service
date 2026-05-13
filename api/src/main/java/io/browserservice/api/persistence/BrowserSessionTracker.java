package io.browserservice.api.persistence;

import io.browserservice.api.persistence.BrowserSessionEntity.ClosedReason;
import io.browserservice.api.persistence.BrowserSessionEntity.Status;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionRegistry;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BrowserSessionTracker {

  private static final Logger log = LoggerFactory.getLogger(BrowserSessionTracker.class);

  private final SessionRegistry registry;

  public BrowserSessionTracker(SessionRegistry registry) {
    this.registry = registry;
  }

  @Transactional
  public void recordCreate(SessionHandle handle) {
    BrowserSessionEntity entity =
        new BrowserSessionEntity(
            handle.id(),
            handle.browserType().name(),
            handle.environment().name(),
            Status.ACTIVE,
            handle.isMobile(),
            handle.createdAt(),
            handle.lastUsedAt(),
            handle.expiresAt(),
            (int) handle.idleTtl().toSeconds(),
            (int) handle.absoluteTtl().toSeconds());
    entity.persist();
  }

  @Transactional
  public void recordClientClose(UUID id) {
    try {
      updateClosed(id, Status.CLOSED, ClosedReason.CLIENT);
    } catch (RuntimeException e) {
      log.warn("failed to record client close for session {}: {}", id, e.toString());
    }
  }

  @Transactional
  public void recordReap(UUID id, ClosedReason reason) {
    try {
      updateClosed(id, Status.EXPIRED, reason);
    } catch (RuntimeException e) {
      log.warn("failed to record reap for session {}: {}", id, e.toString());
    }
  }

  @Scheduled(every = "30s")
  @Transactional
  public void flushLastUsed() {
    List<SessionHandle> snapshot = registry.snapshot();
    if (snapshot.isEmpty()) {
      return;
    }
    Map<UUID, SessionHandle> live = new HashMap<>(snapshot.size());
    for (SessionHandle h : snapshot) {
      if (!h.isClosed()) {
        live.put(h.id(), h);
      }
    }
    if (live.isEmpty()) {
      return;
    }
    log.debug("flushLastUsed: updating last_used_at for {} live sessions", live.size());
    try {
      List<BrowserSessionEntity> rows = BrowserSessionEntity.list("id in ?1", live.keySet());
      for (BrowserSessionEntity entity : rows) {
        if (entity.getStatus() != Status.ACTIVE) {
          continue;
        }
        SessionHandle h = live.get(entity.getId());
        entity.setLastUsedAt(h.lastUsedAt());
        entity.setExpiresAt(h.expiresAt());
      }
    } catch (RuntimeException e) {
      log.warn("failed to flush last_used_at for {} sessions: {}", live.size(), e.toString());
    }
  }

  // Sessions left ACTIVE in the DB by a prior process can never be operated on again — the
  // in-memory WebDriver they reference is gone. Mark them EXPIRED on startup so the table
  // reflects reality.
  @Transactional
  public void recoverOrphanedActiveSessions(@Observes StartupEvent ev) {
    try {
      log.info("startup: scanning browser_sessions for orphaned ACTIVE rows");
      Instant now = Instant.now();
      List<BrowserSessionEntity> orphans = BrowserSessionEntity.list("status", Status.ACTIVE);
      if (orphans.isEmpty()) {
        return;
      }
      for (BrowserSessionEntity entity : orphans) {
        entity.setStatus(Status.EXPIRED);
        entity.setClosedReason(ClosedReason.ERROR);
        entity.setClosedAt(now);
      }
      log.info(
          "marked {} orphaned ACTIVE browser_sessions rows as EXPIRED on startup", orphans.size());
    } catch (RuntimeException e) {
      log.warn("failed to recover orphaned ACTIVE browser_sessions: {}", e.toString());
    }
  }

  private void updateClosed(UUID id, Status status, ClosedReason reason) {
    Optional<BrowserSessionEntity> maybe = BrowserSessionEntity.findByIdOptional(id);
    if (maybe.isEmpty()) {
      log.warn("no browser_sessions row found for id={} while recording {}", id, status);
      return;
    }
    BrowserSessionEntity entity = maybe.get();
    if (entity.getStatus() != Status.ACTIVE) {
      return;
    }
    entity.setStatus(status);
    entity.setClosedReason(reason);
    entity.setClosedAt(Instant.now());
  }
}
