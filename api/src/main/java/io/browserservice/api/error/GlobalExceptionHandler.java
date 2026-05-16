package io.browserservice.api.error;

import io.browserservice.api.dto.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

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
    Map<String, Object> details = mapped.body().details();
    if (details != null) {
      Object retryAfter = details.get(ErrorMapper.RETRY_AFTER_KEY);
      if (retryAfter instanceof Number n) {
        builder.header("Retry-After", String.valueOf(n.intValue()));
      }
    }
    return builder.build();
  }
}
