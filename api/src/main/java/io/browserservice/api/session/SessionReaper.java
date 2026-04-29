package io.browserservice.api.session;

import io.browserservice.api.persistence.BrowserSessionEntity.ClosedReason;
import io.browserservice.api.persistence.BrowserSessionTracker;
import io.browserservice.api.session.SessionHandle.ExpiryReason;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionReaper {

  private static final Logger log = LoggerFactory.getLogger(SessionReaper.class);

  static final String REAPED_COUNTER_NAME = "browserservice.sessions.reaped";
  static final String ACTIVE_GAUGE_NAME = "browserservice.sessions.active";
  static final String REASON_TAG = "reason";

  private final SessionRegistry registry;
  private final BrowserSessionTracker tracker;
  private final Counter reapedIdle;
  private final Counter reapedAbsolute;

  public SessionReaper(
      SessionRegistry registry, BrowserSessionTracker tracker, MeterRegistry meters) {
    this.registry = registry;
    this.tracker = tracker;
    this.reapedIdle =
        Counter.builder(REAPED_COUNTER_NAME)
            .description("Sessions removed by the reaper, tagged by which TTL fired")
            .tag(REASON_TAG, ExpiryReason.IDLE.wireValue())
            .register(meters);
    this.reapedAbsolute =
        Counter.builder(REAPED_COUNTER_NAME)
            .tag(REASON_TAG, ExpiryReason.ABSOLUTE.wireValue())
            .register(meters);
    meters.gauge(ACTIVE_GAUGE_NAME, registry, SessionRegistry::size);
  }

  @Scheduled(fixedDelay = 10_000)
  public void reap() {
    Instant now = Instant.now();
    for (SessionHandle handle : registry.snapshot()) {
      if (handle.isClosed() || !handle.isExpired(now)) {
        continue;
      }
      ExpiryReason reason = handle.expiryReason(now);
      if (registry.remove(handle.id())) {
        tracker.recordReap(handle.id(), toClosedReason(reason));
        (reason == ExpiryReason.ABSOLUTE ? reapedAbsolute : reapedIdle).increment();
        log.info(
            "reaped session id={} owner={} browserType={} reason={} idleTtl={}s absoluteTtl={}s",
            handle.id(),
            handle.owner().value(),
            handle.browserType(),
            reason.wireValue(),
            handle.idleTtl().toSeconds(),
            handle.absoluteTtl().toSeconds());
      }
    }
  }

  private static ClosedReason toClosedReason(ExpiryReason reason) {
    return reason == ExpiryReason.ABSOLUTE
        ? ClosedReason.REAPED_ABSOLUTE
        : ClosedReason.REAPED_IDLE;
  }
}
