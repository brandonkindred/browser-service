package io.browserservice.api.error;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import org.slf4j.MDC;

@Provider
@Priority(Priorities.AUTHENTICATION - 100)
public class RequestIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

  public static final String HEADER = "X-Request-Id";
  public static final String MDC_KEY = "requestId";

  @Override
  public void filter(ContainerRequestContext request) {
    String id = request.getHeaderString(HEADER);
    if (id == null || id.isBlank()) {
      id = UUID.randomUUID().toString();
    }
    MDC.put(MDC_KEY, id);
    request.setProperty(MDC_KEY, id);
  }

  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response) {
    Object id = request.getProperty(MDC_KEY);
    if (id != null) {
      response.getHeaders().putSingle(HEADER, id.toString());
    }
    MDC.remove(MDC_KEY);
  }

  public static String currentRequestId() {
    return MDC.get(MDC_KEY);
  }
}
