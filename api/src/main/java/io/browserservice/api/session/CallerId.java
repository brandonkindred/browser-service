package io.browserservice.api.session;

import java.util.Objects;

/**
 * Composite caller identity derived from a validated OIDC JWT — {@code tenantId} comes from the
 * {@code tenant_id} claim and {@code subject} from the {@code sub} claim. Issue #89 replaced the
 * pre-existing {@code X-Caller-Id} header trust model with this two-part identity so that
 * downstream features (per-tenant rate limiting, audit log, ownership checks) have a meaningful
 * tenant boundary to work against.
 *
 * <p>The canonical wire form returned by {@link #value()} is {@code "<tenantId>:<subject>"}; it is
 * stable for logging and used as the equality / hash basis. Neither half may contain a literal
 * {@code ':'}, so the join is unambiguous.
 */
public final class CallerId {

  /**
   * Maximum length of either half (tenantId or subject) in characters. Matches the OIDC spec's cap
   * on {@code sub} (255 ASCII chars) and the Firebase UID upper bound (128). Smaller caps would
   * silently reject valid tokens from common providers.
   */
  public static final int MAX_HALF_LENGTH = 255;

  /** Maximum length of the canonical {@code value()} string. */
  public static final int MAX_LENGTH = MAX_HALF_LENGTH * 2 + 1;

  private final String tenantId;
  private final String subject;

  private CallerId(String tenantId, String subject) {
    this.tenantId = tenantId;
    this.subject = subject;
  }

  /**
   * Build a {@code CallerId} from JWT claim values. Both halves must be non-blank, ≤{@link
   * #MAX_HALF_LENGTH} chars, printable ASCII, and free of the {@code ':'} delimiter.
   */
  public static CallerId of(String tenantId, String subject) {
    String t = validateHalf(tenantId, "tenant_id");
    String s = validateHalf(subject, "sub");
    return new CallerId(t, s);
  }

  private static String validateHalf(String raw, String claimName) {
    if (raw == null) {
      throw new IllegalArgumentException(claimName + " claim is required");
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(claimName + " claim is required");
    }
    if (trimmed.length() > MAX_HALF_LENGTH) {
      throw new IllegalArgumentException(
          claimName + " claim exceeds " + MAX_HALF_LENGTH + " characters");
    }
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c < 0x21 || c > 0x7E) {
        throw new IllegalArgumentException(
            claimName + " claim contains non-printable or non-ASCII characters");
      }
      if (c == ':') {
        throw new IllegalArgumentException(claimName + " claim must not contain ':'");
      }
    }
    return trimmed;
  }

  public String tenantId() {
    return tenantId;
  }

  public String subject() {
    return subject;
  }

  /** Canonical {@code "<tenantId>:<subject>"} form used for logging and equality. */
  public String value() {
    return tenantId + ":" + subject;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CallerId other)) return false;
    return tenantId.equals(other.tenantId) && subject.equals(other.subject);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantId, subject);
  }

  @Override
  public String toString() {
    return value();
  }
}
