package io.browserservice.api.web;

import io.browserservice.api.error.CallerUnidentifiedException;
import io.browserservice.api.session.CallerId;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Request-scoped view of the authenticated caller. Controllers inject this bean and call {@link
 * #id()} where they previously consumed an {@code @RequestHeader CallerId} parameter.
 *
 * <p>Identity is derived from the validated OIDC token: {@code tenant_id} claim becomes the tenant
 * half and {@code sub} the subject half of {@link CallerId}. Token signature, issuer, audience, and
 * expiry are enforced by quarkus-oidc before any controller runs — this class only deals with the
 * post-validation extraction of the required claims.
 */
@RequestScoped
public class CallerContext {

  private final SecurityIdentity identity;
  private final JsonWebToken token;
  private CallerId cached;

  /** Constructs the context from the request-scoped quarkus-oidc beans. */
  @Inject
  public CallerContext(SecurityIdentity identity, JsonWebToken token) {
    this.identity = identity;
    this.token = token;
  }

  /**
   * Returns the caller identity for the current request, throwing {@link
   * CallerUnidentifiedException} (→ HTTP 401) if the request is anonymous or the token is missing
   * required claims. Result is memoised per request.
   */
  public CallerId id() {
    if (cached == null) {
      cached = resolve();
    }
    return cached;
  }

  private CallerId resolve() {
    if (identity == null || identity.isAnonymous()) {
      throw new CallerUnidentifiedException("authentication required");
    }
    String subject = token.getSubject();
    if (subject == null || subject.isBlank()) {
      throw new CallerUnidentifiedException("missing_subject_claim", "sub claim is missing");
    }
    String tenantId = token.getClaim("tenant_id");
    if (tenantId == null || tenantId.isBlank()) {
      throw new CallerUnidentifiedException("missing_tenant_claim", "tenant_id claim is missing");
    }
    try {
      return CallerId.of(tenantId, subject);
    } catch (IllegalArgumentException e) {
      throw new CallerUnidentifiedException("invalid_claims", e.getMessage(), e);
    }
  }
}
