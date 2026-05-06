package io.browserservice.api.config;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producers the WebSocket layer needs that don't fit naturally inside the {@code @ServerEndpoint}
 * class. Lives in a separate class so the shared scheduler can be injected into both the handler
 * and the watcher coordinator without a construction-time cycle.
 */
@ApplicationScoped
public class WebSocketBeans {

  private final ScheduledExecutorService scheduler;

  public WebSocketBeans() {
    AtomicInteger counter = new AtomicInteger();
    this.scheduler =
        Executors.newScheduledThreadPool(
            2,
            r -> {
              Thread t = new Thread(r, "ws-scheduler-" + counter.incrementAndGet());
              t.setDaemon(true);
              return t;
            });
  }

  @Produces
  @Singleton
  @Named("webSocketScheduler")
  public ScheduledExecutorService webSocketScheduler() {
    return scheduler;
  }

  @PreDestroy
  public void shutdown() {
    scheduler.shutdownNow();
    try {
      scheduler.awaitTermination(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
