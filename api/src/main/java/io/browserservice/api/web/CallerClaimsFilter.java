package io.browserservice.api.web;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Forces the {@link CallerContext} claim check to run before JAX-RS / Spring body binding on every
 * {@code /v1/*} request. Without this filter the resolution lazily fires inside the resource method
 * (via {@code callers.id()}), which means an invalid {@code @RequestBody} would surface as a {@code
 * 400 validation_failed} for a JWT that is missing {@code tenant_id} — contradicting the documented
 * contract that bearer-token failures always return {@code 401}.
 *
 * <p>quarkus-oidc has already validated the token's signature / issuer / audience / expiry by the
 * time JAX-RS filters run (path-permission rule on {@code /v1/*} is {@code authenticated}). This
 * filter only enforces the additional claim contract — {@code sub} present, {@code tenant_id}
 * present and well-formed, both fit {@link io.browserservice.api.session.CallerId#MAX_HALF_LENGTH}.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class CallerClaimsFilter implements ContainerRequestFilter {

  private final CallerContext callers;

  /** Constructs the filter with the request-scoped caller context. */
  @Inject
  public CallerClaimsFilter(CallerContext callers) {
    this.callers = callers;
  }

  @Override
  public void filter(ContainerRequestContext request) {
    if (!requiresCallerCheck(request.getUriInfo().getPath())) {
      return;
    }
    // Triggers resolution; any failure throws CallerUnidentifiedException → 401 via
    // GlobalExceptionHandler, before body binding has a chance to return 400.
    callers.id();
  }

  private static boolean requiresCallerCheck(String path) {
    if (path == null) {
      return false;
    }
    String normalized = path.startsWith("/") ? path.substring(1) : path;
    return "v1".equals(normalized)
        || normalized.startsWith("v1/")
        || normalized.startsWith("wd/hub");
  }
}
