package io.browserservice.api.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class JacksonConfig implements ObjectMapperCustomizer {

  @Override
  public void customize(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    // The HTTP/WS contract is snake_case (session_id, browser_type, owner_id, ...);
    // historically declared in application.yaml as
    // `spring.jackson.property-naming-strategy: SNAKE_CASE`. Quarkus does not honour
    // that Spring key, so we apply the strategy programmatically to keep the wire
    // format stable across the Spring -> Quarkus migration regardless of when the
    // YAML rewrite (#15) lands.
    mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
  }
}
