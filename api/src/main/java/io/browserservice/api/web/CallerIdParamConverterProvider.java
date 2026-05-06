package io.browserservice.api.web;

import io.browserservice.api.error.CallerUnidentifiedException;
import io.browserservice.api.session.CallerId;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Routes the {@code X-Caller-Id} request header into a {@link CallerId} parameter on JAX-RS
 * controller methods. Replaces the Spring {@code HandlerMethodArgumentResolver} pattern used
 * pre-migration. Throws {@link CallerUnidentifiedException} on missing/invalid headers; the global
 * exception mapper translates those to {@code 400}.
 */
@Provider
public class CallerIdParamConverterProvider implements ParamConverterProvider {

  /** HTTP header that carries the caller identifier on every {@code /v1/} request. */
  public static final String HEADER = "X-Caller-Id";

  @Override
  @SuppressWarnings("unchecked")
  public <T> ParamConverter<T> getConverter(
      Class<T> rawType, Type genericType, Annotation[] annotations) {
    if (rawType != CallerId.class) {
      return null;
    }
    return (ParamConverter<T>) CONVERTER;
  }

  private static final ParamConverter<CallerId> CONVERTER =
      new ParamConverter<>() {
        @Override
        public CallerId fromString(String value) {
          if (value == null || value.trim().isEmpty()) {
            throw new CallerUnidentifiedException();
          }
          try {
            return CallerId.parse(value);
          } catch (IllegalArgumentException e) {
            throw new CallerUnidentifiedException(e.getMessage(), e);
          }
        }

        @Override
        public String toString(CallerId value) {
          return value == null ? null : value.value();
        }
      };
}
