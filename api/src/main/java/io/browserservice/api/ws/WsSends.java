package io.browserservice.api.ws;

import jakarta.websocket.CloseReason;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;

/**
 * Bounded WebSocket send helpers shared by the command-response path ({@code
 * SessionWebSocketHandler}) and the watcher push path ({@code EventBroadcaster}).
 *
 * <p>JSR-356 {@code getBasicRemote().sendText/sendBinary} block until the underlying TCP write
 * drains; if a client stops reading, the calling thread parks indefinitely. These helpers issue
 * async sends via {@code getAsyncRemote()} and wait on the returned {@code Future} for at most
 * {@code timeoutMs}. On timeout the future is cancelled, the session is best-effort closed with
 * {@code TRY_AGAIN_LATER} (1013), and the timeout is propagated as {@code IOException} so callers
 * can drop the connection. This bounds how long a stuck client can hold the per-connection command
 * executor or a shared scheduler thread.
 */
public final class WsSends {

  private WsSends() {}

  /** Async-send a text frame, waiting at most {@code timeoutMs}; throws on timeout. */
  public static void sendTextBounded(Connection conn, String text, int timeoutMs, Logger log)
      throws IOException {
    Future<Void> f = conn.out().getAsyncRemote().sendText(text);
    awaitSend(conn, f, timeoutMs, log);
  }

  /** Async-send a binary frame, waiting at most {@code timeoutMs}; throws on timeout. */
  public static void sendBinaryBounded(Connection conn, ByteBuffer buf, int timeoutMs, Logger log)
      throws IOException {
    Future<Void> f = conn.out().getAsyncRemote().sendBinary(buf);
    awaitSend(conn, f, timeoutMs, log);
  }

  private static void awaitSend(Connection conn, Future<Void> f, int timeoutMs, Logger log)
      throws IOException {
    try {
      f.get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      f.cancel(true);
      log.warn("ws send exceeded {}ms, closing connectionId={}", timeoutMs, conn.connectionId());
      try {
        conn.out().close(new CloseReason(CloseReason.CloseCodes.TRY_AGAIN_LATER, "send timeout"));
      } catch (IOException ignored) {
        // best-effort close
      }
      throw new IOException("ws send timed out after " + timeoutMs + "ms", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("ws send interrupted", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException io) {
        io.addSuppressed(e);
        throw io;
      }
      throw new IOException("ws send failed", e);
    }
  }
}
