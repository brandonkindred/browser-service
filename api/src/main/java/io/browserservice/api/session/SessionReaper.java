package io.browserservice.api.session;

import io.browserservice.api.persistence.BrowserSessionEntity.ClosedReason;
import io.browserservice.api.persistence.BrowserSessionTracker;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionReaper {

  private static final Logger log = LoggerFactory.getLogger(SessionReaper.class);

  private final SessionRegistry registry;
  private final BrowserSessionTracker tracker;

  public SessionReaper(SessionRegistry registry, BrowserSessionTracker tracker) {
    this.registry = registry;
    this.tracker = tracker;
  }

  @Scheduled(fixedDelay = 10_000)
  public void reap() {
    Instant now = Instant.now();
    for (SessionHandle handle : registry.snapshot()) {
      if (handle.isClosed() || !handle.isExpired(now)) {
        continue;
      }
      ClosedReason reason = reasonFor(handle);
      if (registry.remove(handle.id())) {
        tracker.recordReap(handle.id(), reason);
        log.info(
            "reaped session id={} browserType={} reason={} idleTtl={}s absoluteTtl={}s",
            handle.id(),
            handle.browserType(),
            reason,
            handle.idleTtl().toSeconds(),
            handle.absoluteTtl().toSeconds());
      }
    }
  }

  private static ClosedReason reasonFor(SessionHandle handle) {
    Instant idleDeadline = handle.lastUsedAt().plus(handle.idleTtl());
    Instant absoluteDeadline = handle.createdAt().plus(handle.absoluteTtl());
    return absoluteDeadline.isBefore(idleDeadline)
        ? ClosedReason.REAPED_ABSOLUTE
        : ClosedReason.REAPED_IDLE;
  }
}
