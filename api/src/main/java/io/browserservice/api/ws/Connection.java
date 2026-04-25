package io.browserservice.api.ws;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/**
 * Per-WebSocket-connection state held in the WebSocketSession attribute map.
 * Mutations to {@link #boundSessionId} happen on the per-connection command executor.
 */
public final class Connection {

    public static final String ATTRIBUTE = "ws.connection";

    private final CallerId caller;
    private final String connectionId;
    private final ConcurrentWebSocketSessionDecorator out;
    private final ExecutorService commands;
    private final Semaphore queue;
    private final AtomicLong lastActivityNanos;
    /**
     * Guards the (binary-header, binary-frame) pair emitted for screenshot ops so it
     * cannot be interleaved on the wire with watcher events from WS-B or with another
     * connection-side write. Single-message writes don't take this lock — the
     * {@link ConcurrentWebSocketSessionDecorator} is already thread-safe per message.
     */
    private final Object writeLock = new Object();
    private volatile UUID boundSessionId;

    public Connection(CallerId caller, String connectionId, ConcurrentWebSocketSessionDecorator out,
                      ExecutorService commands, Semaphore queue) {
        this.caller = caller;
        this.connectionId = connectionId;
        this.out = out;
        this.commands = commands;
        this.queue = queue;
        this.lastActivityNanos = new AtomicLong(System.nanoTime());
    }

    public CallerId caller() { return caller; }
    public String connectionId() { return connectionId; }
    public ConcurrentWebSocketSessionDecorator out() { return out; }
    public ExecutorService commands() { return commands; }
    public Semaphore queue() { return queue; }
    public Object writeLock() { return writeLock; }
    public UUID boundSessionId() { return boundSessionId; }
    public void bind(UUID sessionId) { this.boundSessionId = sessionId; }
    public void unbind() { this.boundSessionId = null; }

    public void touchActivity() {
        this.lastActivityNanos.set(System.nanoTime());
    }

    public long lastActivityNanos() {
        return lastActivityNanos.get();
    }
}
