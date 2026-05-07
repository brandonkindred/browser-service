package io.browserservice.api.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

/**
 * Canonical Jackson configuration for the Quarkus runtime: snake_case property naming, ISO-8601
 * dates (not numeric timestamps), and {@code non_null} serialization inclusion. Spring's {@code
 * spring.jackson.*} YAML keys aren't honoured by Quarkus, so the wire-format settings live here in
 * code rather than {@code application.yaml}.
 */
@Singleton
public class JacksonConfig implements ObjectMapperCustomizer {

  @Override
  public void customize(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }
}
