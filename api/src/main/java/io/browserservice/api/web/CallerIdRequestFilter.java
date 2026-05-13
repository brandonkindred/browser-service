package io.browserservice.api.web;

import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.error.CallerUnidentifiedException;
import io.browserservice.api.error.ErrorMapper;
import io.browserservice.api.error.RequestIdFilter;
import io.browserservice.api.session.CallerId;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Enforces presence and validity of the {@code X-Caller-Id} header on every {@code /v1/*} request.
 * The {@link CallerIdParamConverterProvider} is not invoked by {@code quarkus-spring-web} for
 * {@code @RequestHeader CallerId} parameters, so this filter is the authoritative gate; controllers
 * may still rely on the parsed value via standard JAX-RS binding (see {@link
 * CallerId#valueOf(String)}).
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class CallerIdRequestFilter implements ContainerRequestFilter {

  public static final String REQUEST_PROPERTY = "io.browserservice.callerId";

  @Override
  public void filter(ContainerRequestContext request) {
    if (!isV1Path(request.getUriInfo().getPath())) {
      return;
    }

    String raw = request.getHeaderString(CallerIdParamConverterProvider.HEADER);
    if (raw == null || raw.isBlank()) {
      abort(request, new CallerUnidentifiedException());
      return;
    }
    try {
      CallerId parsed = CallerId.parse(raw);
      request.setProperty(REQUEST_PROPERTY, parsed);
    } catch (IllegalArgumentException e) {
      abort(request, new CallerUnidentifiedException(e.getMessage(), e));
    }
  }

  private static boolean isV1Path(String path) {
    if (path == null) {
      return false;
    }
    String normalized = path.startsWith("/") ? path.substring(1) : path;
    return "v1".equals(normalized) || normalized.startsWith("v1/");
  }

  private static void abort(ContainerRequestContext request, CallerUnidentifiedException ex) {
    String rid = RequestIdFilter.currentRequestId();
    ErrorMapper.Mapped mapped = ErrorMapper.map(ex, rid);
    Response.ResponseBuilder builder =
        Response.status(mapped.status().value())
            .entity(new ErrorResponse(mapped.body()))
            .type(MediaType.APPLICATION_JSON);
    if (rid != null) {
      builder.header(RequestIdFilter.HEADER, rid);
    }
    request.abortWith(builder.build());
  }
}
