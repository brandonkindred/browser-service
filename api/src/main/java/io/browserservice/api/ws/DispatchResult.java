package io.browserservice.api.ws;

/**
 * Outcome of a single WS command. The handler in {@code SessionWebSocketHandler} switches on the
 * variant: {@link Json} writes one {@code response} frame, {@link Binary} writes a {@code
 * binary-header} JSON frame followed by a single WS binary frame containing exactly {@code
 * bytes.length} bytes.
 */
public sealed interface DispatchResult {

  /** A regular JSON {@code response} payload — what every WS-A/WS-B op returns. */
  record Json(Object value) implements DispatchResult {}

  /** A binary payload that rides as a (header, bytes) pair on the wire. */
  record Binary(String mime, byte[] bytes) implements DispatchResult {}
}
