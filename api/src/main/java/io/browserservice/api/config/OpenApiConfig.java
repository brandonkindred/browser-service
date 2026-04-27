package io.browserservice.api.config;

import io.browserservice.api.session.CallerId;
import io.browserservice.api.web.CallerIdArgumentResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
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

  /**
   * Documents {@code X-Caller-Id} as a required header on every operation under {@code /v1/}.
   * Endpoints outside that prefix (e.g. {@code /healthz}, {@code /readyz}) are unaffected.
   */
  @Bean
  public OpenApiCustomizer callerIdHeaderCustomizer() {
    return openApi -> {
      if (openApi.getPaths() == null) {
        return;
      }
      openApi
          .getPaths()
          .forEach(
              (path, pathItem) -> {
                if (path == null || !path.startsWith("/v1/")) {
                  return;
                }
                for (Operation op : operationsOf(pathItem)) {
                  if (hasCallerIdHeader(op)) {
                    continue;
                  }
                  op.addParametersItem(callerIdHeaderParameter());
                }
              });
    };
  }

  private static List<Operation> operationsOf(PathItem item) {
    return item.readOperations();
  }

  private static boolean hasCallerIdHeader(Operation op) {
    if (op.getParameters() == null) {
      return false;
    }
    for (Parameter p : op.getParameters()) {
      if ("header".equals(p.getIn())
          && CallerIdArgumentResolver.CALLER_HEADER.equals(p.getName())) {
        return true;
      }
    }
    return false;
  }

  private static Parameter callerIdHeaderParameter() {
    return new HeaderParameter()
        .name(CallerIdArgumentResolver.CALLER_HEADER)
        .required(true)
        .description("Identifies the calling client. Bound to created sessions for ownership.")
        .schema(new StringSchema().maxLength(CallerId.MAX_LENGTH));
  }
}
