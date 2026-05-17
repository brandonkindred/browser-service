package io.browserservice.api.session;

import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.error.SessionCapExceededException;
import io.browserservice.api.error.SessionNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Component;

/**
 * In-memory registry of live browser sessions for the current JVM.
 *
 * <p>State lives entirely on the heap: a {@link ConcurrentHashMap} of {@link SessionHandle}s (each
 * holding a live Selenium {@code WebDriver}, a {@link java.util.concurrent.locks.ReentrantLock},
 * and other non-serializable references) plus a {@link Semaphore} that enforces the per-pod
 * concurrent-session cap. None of this state is shared between pods.
 *
 * <p><b>Single-pod constraint.</b> Because session state is per-JVM, the service MUST run with
 * exactly one Cloud Run instance — neither horizontal scale-out ({@code max_instances > 1}) nor
 * scale-to-zero ({@code min_instances = 0}) is safe. With {@code max_instances > 1}, follow-up
 * requests ({@code /sessions/{id}/navigate}, screenshot, close, …) routed by the load balancer to a
 * different pod fail with {@code SessionNotFoundException}. With {@code min_instances = 0}, the JVM
 * (and this map) is discarded between requests, so a follow-up call cold-starts a fresh instance
 * with an empty registry — same failure mode. Both bounds are pinned at the infrastructure layer by
 * Terraform validations on {@code browser_service_min_instances} and {@code
 * browser_service_max_instances} ({@code terraform/variables.tf}).
 *
 * <p>Lifting this constraint is tracked by issue #119 (R10 Phase 1): externalize the registry to
 * Redis and either route by session-affinity or broker WebDriver access via Selenium Grid. See
 * {@code docs/capacity.md} for the operator-facing summary.
 */
@Component
public class SessionRegistry {

  private final ConcurrentHashMap<UUID, SessionHandle> sessions = new ConcurrentHashMap<>();
  private final Semaphore capacity;
  private final int maxConcurrent;

  public SessionRegistry(EngineProperties props) {
    this.maxConcurrent = props.session().maxConcurrent();
    this.capacity = new Semaphore(this.maxConcurrent);
  }

  public void acquirePermit() {
    if (!capacity.tryAcquire()) {
      throw new SessionCapExceededException(maxConcurrent);
    }
  }

  public void releasePermit() {
    capacity.release();
  }

  public void register(SessionHandle handle) {
    sessions.put(handle.id(), handle);
  }

  public SessionHandle get(UUID id) {
    SessionHandle h = sessions.get(id);
    if (h == null || h.isClosed()) {
      throw new SessionNotFoundException(id);
    }
    return h;
  }

  public Optional<SessionHandle> find(UUID id) {
    SessionHandle h = sessions.get(id);
    if (h == null || h.isClosed()) {
      return Optional.empty();
    }
    return Optional.of(h);
  }

  public List<SessionHandle> snapshot() {
    return List.copyOf(sessions.values());
  }

  public boolean remove(UUID id) {
    SessionHandle h = sessions.remove(id);
    if (h == null) {
      return false;
    }
    boolean wasOpen = h.closeOnce();
    if (wasOpen) {
      capacity.release();
    }
    return wasOpen;
  }

  public int size() {
    return (int) sessions.values().stream().filter(s -> !s.isClosed()).count();
  }

  public int availablePermits() {
    return capacity.availablePermits();
  }

  public int maxConcurrent() {
    return maxConcurrent;
  }
}
