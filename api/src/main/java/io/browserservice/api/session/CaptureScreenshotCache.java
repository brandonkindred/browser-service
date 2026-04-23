package io.browserservice.api.session;

import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.error.CaptureExpiredException;
import io.browserservice.api.error.CaptureNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CaptureScreenshotCache {

    public record CaptureEntry(byte[] pngBytes, int width, int height, Instant expiresAt) {
    }

    private final ConcurrentHashMap<UUID, CaptureEntry> entries = new ConcurrentHashMap<>();
    private final Duration defaultTtl;

    public CaptureScreenshotCache(EngineProperties props) {
        this.defaultTtl = Duration.ofSeconds(props.session().absoluteTtlSeconds());
    }

    public UUID put(byte[] pngBytes, int width, int height) {
        return put(pngBytes, width, height, defaultTtl);
    }

    public UUID put(byte[] pngBytes, int width, int height, Duration ttl) {
        UUID id = UUID.randomUUID();
        entries.put(id, new CaptureEntry(pngBytes, width, height, Instant.now().plus(ttl)));
        return id;
    }

    public CaptureEntry get(UUID id) {
        CaptureEntry entry = entries.get(id);
        if (entry == null) {
            throw new CaptureNotFoundException(id);
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            entries.remove(id);
            throw new CaptureExpiredException(id);
        }
        return entry;
    }

    public Duration defaultTtl() {
        return defaultTtl;
    }

    @Scheduled(fixedDelay = 30_000)
    public void reap() {
        Instant now = Instant.now();
        entries.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}
