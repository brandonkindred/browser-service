package io.browserservice.api.session;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionReaper {

    private static final Logger log = LoggerFactory.getLogger(SessionReaper.class);

    private final SessionRegistry registry;

    public SessionReaper(SessionRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelay = 10_000)
    public void reap() {
        Instant now = Instant.now();
        for (SessionHandle handle : registry.snapshot()) {
            if (handle.isClosed() || !handle.isExpired(now)) {
                continue;
            }
            if (registry.remove(handle.id())) {
                log.info("reaped session id={} browserType={} idleTtl={}s absoluteTtl={}s",
                        handle.id(), handle.browserType(),
                        handle.idleTtl().toSeconds(), handle.absoluteTtl().toSeconds());
            }
        }
    }
}
