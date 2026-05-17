package io.browserservice.api.ws;

import io.browserservice.api.session.CallerId;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Validates the JWT bearer token presented by a WebSocket client on the upgrade request and rejects
 * the handshake with HTTP 401 when it is missing, malformed, or unverifiable.
 *
 * <p>Browsers cannot set the {@code Authorization} header on a WebSocket upgrade, so clients pass
 * the token through the {@code Sec-WebSocket-Protocol} subprotocol negotiation: the offered
 * subprotocols are {@code "bearer"} (a sentinel echoed back on success) and the raw JWT itself.
 * This matches the convention popularised by Kubernetes' API server and RFC 8307.
 *
 * <p>The token is validated with smallrye-jwt's {@link JWTParser}, configured to use the same
 * issuer/audience/JWKS as {@code quarkus-oidc} (see {@code mp.jwt.*} in application.yaml). On
 * success the upgrade is permitted; {@link SessionSocket#onOpen()} re-parses the same token to
 * derive the {@code CallerId}, which is cheap because the JWKS is cached.
 */
@ApplicationScoped
public class CallerIdUpgradeCheck implements HttpUpgradeCheck {

  /** Standard WebSocket subprotocol negotiation header (RFC 6455 §11.3.4). */
  public static final String SUBPROTOCOL_HEADER = "Sec-WebSocket-Protocol";

  /** Sentinel subprotocol value paired with the JWT; echoed back to the client on success. */
  public static final String BEARER_SUBPROTOCOL = "bearer";

  private final JWTParser jwtParser;

  /** Constructs the check with the smallrye-jwt parser used to validate the subprotocol token. */
  @Inject
  public CallerIdUpgradeCheck(JWTParser jwtParser) {
    this.jwtParser = jwtParser;
  }

  @Override
  public Uni<CheckResult> perform(HttpUpgradeContext ctx) {
    String rawSubprotocols = ctx.httpRequest().getHeader(SUBPROTOCOL_HEADER);
    String token = extractBearerToken(rawSubprotocols);
    if (token == null) {
      return CheckResult.rejectUpgrade(401);
    }
    try {
      JsonWebToken parsed = jwtParser.parse(token);
      // tenant_id claim must be a non-blank string — defend against a number/array/object value
      // that would otherwise throw ClassCastException at the implicit assignment and surface as a
      // 500 from the upgrade pipeline.
      Object tenantClaim = parsed.getClaim("tenant_id");
      if (!(tenantClaim instanceof String tenant)) {
        return CheckResult.rejectUpgrade(401);
      }
      // Reuse CallerId.of so the upgrade check rejects with 401 the same tokens the REST path
      // would reject. Without this, claims that pass the simple presence check but fail the
      // length / ASCII / no-colon rules in CallerId.of would let the WS upgrade complete and
      // then close 4401 in SessionSocket.onOpen — clients would see a successful handshake
      // followed by an immediate close instead of the documented 401 upgrade rejection.
      CallerId.of(tenant, parsed.getSubject());
    } catch (ParseException | IllegalArgumentException e) {
      return CheckResult.rejectUpgrade(401);
    }
    // Echo the negotiated subprotocol back to the client so the WebSocket handshake completes
    // protocol negotiation correctly per RFC 6455 §11.3.4 — without this, some clients abort the
    // upgrade because the server failed to acknowledge the requested subprotocol.
    return CheckResult.permitUpgrade(Map.of(SUBPROTOCOL_HEADER, List.of(BEARER_SUBPROTOCOL)));
  }

  /**
   * Pulls the JWT out of a comma-separated {@code Sec-WebSocket-Protocol} value. The first
   * non-{@code bearer} value is treated as the token; absence of either part returns {@code null}.
   */
  static String extractBearerToken(String rawSubprotocols) {
    if (rawSubprotocols == null || rawSubprotocols.isBlank()) {
      return null;
    }
    boolean sawBearer = false;
    String candidate = null;
    for (String s : List.of(rawSubprotocols.split(","))) {
      String trimmed = s.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (BEARER_SUBPROTOCOL.equalsIgnoreCase(trimmed)) {
        sawBearer = true;
      } else if (candidate == null) {
        candidate = trimmed;
      }
    }
    return sawBearer ? candidate : null;
  }
}
