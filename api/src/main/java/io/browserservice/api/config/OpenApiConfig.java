package io.browserservice.api.config;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

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

  /** Name of the {@code components.securitySchemes} entry referenced by /v1 operations. */
  public static final String BEARER_AUTH_SCHEME = "bearerAuth";

  /**
   * Declares a single {@code bearerAuth} OIDC JWT security scheme and requires it on every
   * operation under {@code /v1/}. Endpoints outside that prefix (e.g. {@code /healthz}, {@code
   * /readyz}, {@code /metrics}) remain unauthenticated and therefore carry no security requirement.
   */
  @Override
  public void filterOpenAPI(OpenAPI openApi) {
    Components components = openApi.getComponents();
    if (components == null) {
      components = OASFactory.createComponents();
      openApi.setComponents(components);
    }
    // SmallRye returns Collections.unmodifiableMap() from getSecuritySchemes() when the model
    // already has any entries — copy into a fresh map before mutating to avoid UOE at startup.
    Map<String, SecurityScheme> existing = components.getSecuritySchemes();
    Map<String, SecurityScheme> schemes =
        existing == null ? new HashMap<>() : new HashMap<>(existing);
    schemes.put(BEARER_AUTH_SCHEME, bearerJwtScheme());
    components.setSecuritySchemes(schemes);

    if (openApi.getPaths() == null || openApi.getPaths().getPathItems() == null) {
      return;
    }
    openApi
        .getPaths()
        .getPathItems()
        .forEach(
            (path, pathItem) -> {
              if (path == null || !path.startsWith("/v1/")) {
                return;
              }
              for (Operation op : operationsOf(pathItem)) {
                if (op == null) {
                  continue;
                }
                requireBearerAuth(op);
              }
            });
  }

  private static SecurityScheme bearerJwtScheme() {
    return OASFactory.createSecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .description("OIDC-signed JWT bearer token. Required on every /v1/ request.");
  }

  private static void requireBearerAuth(Operation op) {
    List<SecurityRequirement> existing = op.getSecurity();
    List<SecurityRequirement> security = existing == null ? new ArrayList<>() : existing;
    boolean alreadyRequired =
        security.stream().anyMatch(req -> req.getScheme(BEARER_AUTH_SCHEME) != null);
    if (!alreadyRequired) {
      security.add(OASFactory.createSecurityRequirement().addScheme(BEARER_AUTH_SCHEME));
    }
    op.setSecurity(security);
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
}
