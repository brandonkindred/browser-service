package io.browserservice.api.error;

import io.browserservice.api.dto.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import java.util.Set;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

  // Whitelist of error codes whose ErrorDetail.details may carry a retry_after_seconds hint.
  // Keeping this explicit (rather than blanket-reading details on every error) prevents a future
  // ApiException that happens to put "retry_after_seconds" in details from accidentally growing a
  // Retry-After header.
  private static final Set<String> RETRY_AFTER_CODES = Set.of("selenium_circuit_open");

  @Override
  public Response toResponse(Throwable ex) {
    String rid = RequestIdFilter.currentRequestId();
    ErrorMapper.Mapped mapped = ErrorMapper.map(ex, rid);
    Response.ResponseBuilder builder =
        Response.status(mapped.status().value())
            .entity(new ErrorResponse(mapped.body()))
            .type(MediaType.APPLICATION_JSON);
    if (rid != null) {
      builder.header(RequestIdFilter.HEADER, rid);
    }
    if (RETRY_AFTER_CODES.contains(mapped.body().code())) {
      Map<String, Object> details = mapped.body().details();
      if (details != null && details.get(ErrorMapper.RETRY_AFTER_KEY) instanceof Number n) {
        builder.header("Retry-After", String.valueOf(n.intValue()));
      }
    }
    return builder.build();
  }
}
