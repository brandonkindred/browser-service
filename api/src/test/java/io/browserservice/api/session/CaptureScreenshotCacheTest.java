package io.browserservice.api.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.error.CaptureExpiredException;
import io.browserservice.api.error.CaptureNotFoundException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CaptureScreenshotCacheTest {

    @Test
    void putAndGetRoundTripsWithDefaultTtl() {
        CaptureScreenshotCache cache = new CaptureScreenshotCache(props(60));
        byte[] payload = new byte[]{1, 2, 3};

        UUID id = cache.put(payload, 100, 50);
        CaptureScreenshotCache.CaptureEntry entry = cache.get(id);

        assertThat(entry.pngBytes()).isEqualTo(payload);
        assertThat(entry.width()).isEqualTo(100);
        assertThat(entry.height()).isEqualTo(50);
        assertThat(cache.defaultTtl()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void getUnknownIdThrowsCaptureNotFound() {
        CaptureScreenshotCache cache = new CaptureScreenshotCache(props(60));
        assertThatThrownBy(() -> cache.get(UUID.randomUUID()))
                .isInstanceOf(CaptureNotFoundException.class);
    }

    @Test
    void expiredEntryThrowsCaptureExpired() throws InterruptedException {
        CaptureScreenshotCache cache = new CaptureScreenshotCache(props(60));
        UUID id = cache.put(new byte[]{9}, 1, 1, Duration.ofMillis(1));
        Thread.sleep(5);

        assertThatThrownBy(() -> cache.get(id)).isInstanceOf(CaptureExpiredException.class);
        // Second fetch should now be "not found" — the expired entry was evicted.
        assertThatThrownBy(() -> cache.get(id)).isInstanceOf(CaptureNotFoundException.class);
    }

    @Test
    void reapRemovesExpiredEntries() throws InterruptedException {
        CaptureScreenshotCache cache = new CaptureScreenshotCache(props(60));
        UUID expired = cache.put(new byte[]{1}, 1, 1, Duration.ofMillis(1));
        UUID alive = cache.put(new byte[]{2}, 1, 1, Duration.ofSeconds(60));
        Thread.sleep(5);

        cache.reap();

        assertThatThrownBy(() -> cache.get(expired)).isInstanceOf(CaptureNotFoundException.class);
        assertThat(cache.get(alive).pngBytes()).isEqualTo(new byte[]{2});
    }

    private static EngineProperties props(int absoluteSeconds) {
        return new EngineProperties(
                new EngineProperties.SessionProps(10, absoluteSeconds, 5, 1000),
                new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000));
    }
}
