package io.browserservice.api.ws;

import io.browserservice.api.session.CallerId;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-WebSocket-connection state held by the {@link SessionSocket} bean for the lifetime of one
 * Quarkus {@code @SessionScoped} WS connection. Mutations to {@link #boundSessionId} happen on the
 * per-connection command executor.
 *
 * <p>Holds the Quarkus connection id ({@link #wsConnectionId}) rather than a reference to the
 * injected {@code WebSocketConnection} proxy: that proxy resolves through the
 * {@code @SessionScoped} CDI context, which is only active during endpoint callback methods.
 * Background threads (the per-connection command executor, the idle watchdog, the watcher
 * scheduler) look the live connection up via {@code OpenConnections.findByConnectionId(...)}
 * instead.
 */
public final class WsConnectionState {

  private final CallerId caller;
  private final String connectionId;
  private final String wsConnectionId;
  private final ExecutorService commands;
  private final Semaphore queue;
  private final AtomicLong lastActivityNanos;

  /**
   * Guards the (binary-header, binary-frame) pair emitted for screenshot ops so it cannot be
   * interleaved on the wire with watcher events or with another connection-side write. All outbound
   * sends must be serialized through this lock.
   */
  private final Object writeLock = new Object();

  private volatile UUID boundSessionId;

  public WsConnectionState(
      CallerId caller,
      String connectionId,
      String wsConnectionId,
      ExecutorService commands,
      Semaphore queue) {
    this.caller = caller;
    this.connectionId = connectionId;
    this.wsConnectionId = wsConnectionId;
    this.commands = commands;
    this.queue = queue;
    this.lastActivityNanos = new AtomicLong(System.nanoTime());
  }

  public CallerId caller() {
    return caller;
  }

  public String connectionId() {
    return connectionId;
  }

  /** Quarkus' WS connection id, used to look the live connection back up off the callback. */
  public String wsConnectionId() {
    return wsConnectionId;
  }

  public ExecutorService commands() {
    return commands;
  }

  public Semaphore queue() {
    return queue;
  }

  public Object writeLock() {
    return writeLock;
  }

  public UUID boundSessionId() {
    return boundSessionId;
  }

  public void bind(UUID sessionId) {
    this.boundSessionId = sessionId;
  }

  public void unbind() {
    this.boundSessionId = null;
  }

  public void touchActivity() {
    this.lastActivityNanos.set(System.nanoTime());
  }

  public long lastActivityNanos() {
    return lastActivityNanos.get();
  }
}
