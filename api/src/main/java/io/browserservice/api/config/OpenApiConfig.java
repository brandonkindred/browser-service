package io.browserservice.api.config;

import io.browserservice.api.session.CallerId;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

@OpenAPIDefinition(
    info =
        @Info(
            title = "Browser Service",
            version = "v1",
            description =
                "Standalone service that exposes Selenium/Appium browser sessions over HTTP. "
                    + "Remote browser sessions for programmatic web interaction.",
            license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")),
    servers = {
      @Server(url = "http://browser-service.internal/v1", description = "Internal VPC endpoint"),
      @Server(url = "http://localhost:8080/v1", description = "Local development")
    },
    tags = {
      @Tag(name = "Sessions", description = "Session lifecycle"),
      @Tag(name = "Navigation", description = "Page navigation and source"),
      @Tag(name = "Screenshots", description = "Viewport, full-page, and element screenshots"),
      @Tag(name = "Elements", description = "Find elements and perform actions on them"),
      @Tag(name = "Touch", description = "Mobile touch gestures"),
      @Tag(name = "Scrolling", description = "Viewport scrolling operations"),
      @Tag(name = "DOM", description = "Direct DOM manipulation helpers"),
      @Tag(name = "Alerts", description = "Browser alert detection and response"),
      @Tag(name = "Mouse", description = "Desktop mouse operations"),
      @Tag(name = "Script", description = "Arbitrary JavaScript execution"),
      @Tag(name = "Capture", description = "One-shot navigate + capture + close"),
      @Tag(name = "Ops", description = "Health, readiness, metrics")
    })
@OpenApiFilter
public class OpenApiConfig implements OASFilter {

  public static final String CALLER_HEADER = "X-Caller-Id";

  /**
   * Documents {@code X-Caller-Id} as a required header on every operation under {@code /v1/}.
   * Endpoints outside that prefix (e.g. {@code /healthz}, {@code /readyz}) are unaffected.
   */
  @Override
  public void filterOpenAPI(OpenAPI openAPI) {
    if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems() == null) {
      return;
    }
    openAPI
        .getPaths()
        .getPathItems()
        .forEach(
            (path, pathItem) -> {
              if (path == null || !path.startsWith("/v1/")) {
                return;
              }
              for (Operation op : operationsOf(pathItem)) {
                if (op == null || hasCallerIdHeader(op)) {
                  continue;
                }
                op.addParameter(callerIdHeaderParameter());
              }
            });
  }

  private static Operation[] operationsOf(PathItem item) {
    return new Operation[] {
      item.getGET(),
      item.getPUT(),
      item.getPOST(),
      item.getDELETE(),
      item.getOPTIONS(),
      item.getHEAD(),
      item.getPATCH(),
      item.getTRACE()
    };
  }

  private static boolean hasCallerIdHeader(Operation op) {
    if (op.getParameters() == null) {
      return false;
    }
    for (Parameter p : op.getParameters()) {
      if (Parameter.In.HEADER.equals(p.getIn()) && CALLER_HEADER.equals(p.getName())) {
        return true;
      }
    }
    return false;
  }

  private static Parameter callerIdHeaderParameter() {
    return OASFactory.createParameter()
        .name(CALLER_HEADER)
        .in(Parameter.In.HEADER)
        .required(true)
        .description("Identifies the calling client. Bound to created sessions for ownership.")
        .schema(
            OASFactory.createSchema().type(Schema.SchemaType.STRING).maxLength(CallerId.MAX_LENGTH));
  }
}
