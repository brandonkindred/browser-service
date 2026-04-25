package io.browserservice.api.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans the WebSocket layer needs that don't fit naturally inside {@link WebSocketConfig}
 * (which already has a constructor dependency on {@code SessionWebSocketHandler}). Lives
 * in a separate config class so the shared scheduler can be injected into both the
 * handler and the watcher coordinator without a construction-time cycle.
 */
@Configuration
public class WebSocketBeans {

    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService webSocketScheduler() {
        AtomicInteger counter = new AtomicInteger();
        return Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "ws-scheduler-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }
}
