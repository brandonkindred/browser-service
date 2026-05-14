package io.browserservice.api.ws;

import io.browserservice.api.session.CallerId;
import io.quarkus.websockets.next.WebSocketConnection;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-WebSocket-connection state held by the {@link SessionSocket} bean for the lifetime of one
 * Quarkus {@code @SessionScoped} WS connection. Mutations to {@link #boundSessionId} happen on the
 * per-connection command executor.
 */
public final class WsConnectionState {

  private final CallerId caller;
  private final String connectionId;
  private final WebSocketConnection out;
  private final ExecutorService commands;
  private final Semaphore queue;
  private final AtomicLong lastActivityNanos;

  /**
   * Guards the (binary-header, binary-frame) pair emitted for screenshot ops so it cannot be
   * interleaved on the wire with watcher events or with another connection-side write. All writes
   * to {@link #out} must be serialized through this lock.
   */
  private final Object writeLock = new Object();

  private volatile UUID boundSessionId;

  public WsConnectionState(
      CallerId caller,
      String connectionId,
      WebSocketConnection out,
      ExecutorService commands,
      Semaphore queue) {
    this.caller = caller;
    this.connectionId = connectionId;
    this.out = out;
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

  public WebSocketConnection out() {
    return out;
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
