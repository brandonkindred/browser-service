package io.browserservice.api.error;

import io.browserservice.api.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

  @Override
  public Response toResponse(Throwable ex) {
    ErrorMapper.Mapped mapped = ErrorMapper.map(ex, RequestIdFilter.currentRequestId());
    return Response.status(mapped.status().value())
        .entity(new ErrorResponse(mapped.body()))
        .build();
  }
}
