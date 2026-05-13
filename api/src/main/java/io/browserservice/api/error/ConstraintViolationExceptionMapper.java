package io.browserservice.api.error;

import io.browserservice.api.dto.ErrorResponse;
import jakarta.annotation.Priority;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Wins over RESTEasy's built-in {@link ConstraintViolationException} mapper (which emits the
 * RFC-7807 "Constraint Violation" body); lower {@link Priority} ranks higher in JAX-RS mapper
 * selection, so the project's {@code ErrorResponse} envelope is what clients actually see.
 */
@Provider
@Priority(Priorities.USER - 100)
public class ConstraintViolationExceptionMapper
    implements ExceptionMapper<ConstraintViolationException> {

  /** Maps a constraint-violation failure to the canonical {@code validation_failed} body. */
  @Override
  public Response toResponse(ConstraintViolationException ex) {
    String rid = RequestIdFilter.currentRequestId();
    ErrorMapper.Mapped mapped = ErrorMapper.map(ex, rid);
    Response.ResponseBuilder builder =
        Response.status(mapped.status().value())
            .entity(new ErrorResponse(mapped.body()))
            .type(MediaType.APPLICATION_JSON);
    if (rid != null) {
      builder.header(RequestIdFilter.HEADER, rid);
    }
    return builder.build();
  }
}
