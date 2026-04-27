package io.browserservice.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI browserServiceOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Browser Service")
                .version("v1")
                .summary("Remote browser sessions for programmatic web interaction.")
                .description(
                    "Standalone service that exposes Selenium/Appium browser sessions over HTTP.")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
        .servers(
            List.of(
                new Server()
                    .url("http://browser-service.internal/v1")
                    .description("Internal VPC endpoint"),
                new Server().url("http://localhost:8080/v1").description("Local development")))
        .tags(
            List.of(
                new Tag().name("Sessions").description("Session lifecycle"),
                new Tag().name("Navigation").description("Page navigation and source"),
                new Tag()
                    .name("Screenshots")
                    .description("Viewport, full-page, and element screenshots"),
                new Tag().name("Elements").description("Find elements and perform actions on them"),
                new Tag().name("Touch").description("Mobile touch gestures"),
                new Tag().name("Scrolling").description("Viewport scrolling operations"),
                new Tag().name("DOM").description("Direct DOM manipulation helpers"),
                new Tag().name("Alerts").description("Browser alert detection and response"),
                new Tag().name("Mouse").description("Desktop mouse operations"),
                new Tag().name("Script").description("Arbitrary JavaScript execution"),
                new Tag().name("Capture").description("One-shot navigate + capture + close"),
                new Tag().name("Ops").description("Health, readiness, metrics")))
        .components(new Components());
  }
}
