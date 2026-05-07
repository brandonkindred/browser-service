package io.browserservice.api.config;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.util.List;

/**
 * Tags every metric with {@code application=browser-service-api} so Prometheus scrapes can
 * distinguish this service from siblings sharing a registry. Replaces the Spring-only {@code
 * management.metrics.tags.application} YAML key, which Quarkus does not honour.
 */
public class MicrometerCommonTagsConfig {

  /** Produces the common-tag filter the micrometer extension applies to every registered meter. */
  @Produces
  @Singleton
  public MeterFilter applicationTagFilter() {
    return MeterFilter.commonTags(List.of(Tag.of("application", "browser-service-api")));
  }
}
