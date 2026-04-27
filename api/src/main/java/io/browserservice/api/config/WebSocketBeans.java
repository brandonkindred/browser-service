package io.browserservice.api.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans the WebSocket layer needs that don't fit naturally inside {@link WebSocketConfig} (which
 * already has a constructor dependency on {@code SessionWebSocketHandler}). Lives in a separate
 * config class so the shared scheduler can be injected into both the handler and the watcher
 * coordinator without a construction-time cycle.
 */
@Configuration
public class WebSocketBeans {

  @Bean(destroyMethod = "shutdownNow")
  public ScheduledExecutorService webSocketScheduler() {
    AtomicInteger counter = new AtomicInteger();
    return Executors.newScheduledThreadPool(
        2,
        r -> {
          Thread t = new Thread(r, "ws-scheduler-" + counter.incrementAndGet());
          t.setDaemon(true);
          return t;
        });
  }

  // Note on inbound binary buffer size: WS-C is server-emit-only — clients never send
  // binary frames today. The sole emission ceiling is enforced per-message in
  // SessionWebSocketHandler.writeBinaryPair (oversize → screenshot_too_large error
  // frame, no binary). A `ServletServerContainerFactoryBean` would be the right place
  // to bound inbound buffers if/when binary uploads land, but it requires a real
  // servlet ServerContainer at boot, which would break MOCK-environment tests.
}
