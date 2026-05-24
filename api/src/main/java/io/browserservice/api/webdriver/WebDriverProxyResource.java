package io.browserservice.api.webdriver;

import io.browserservice.api.web.CallerContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * W3C WebDriver protocol proxy. Clients point their {@code RemoteWebDriver} at this endpoint (e.g.
 * {@code http://host:8080/wd/hub}) just like they would BrowserStack or Sauce Labs. The service
 * authenticates via OIDC bearer token, enforces session ownership and quotas, then proxies the
 * WebDriver commands to the underlying Selenium Grid.
 *
 * <p>Usage from a client:
 *
 * <pre>{@code
 * ChromeOptions options = new ChromeOptions();
 * RemoteWebDriver driver = new RemoteWebDriver(
 *     new URL("http://browser-service:8080/wd/hub"), options);
 * driver.get("http://example.com");
 * }</pre>
 *
 * <p>Authentication: pass an OIDC bearer token in the standard {@code Authorization} header.
 * Selenium's {@code RemoteWebDriver} supports custom headers via {@code ClientConfig}:
 *
 * <pre>{@code
 * ClientConfig config = ClientConfig.defaultConfig()
 *     .baseUrl(new URL("http://browser-service:8080/wd/hub"))
 *     .authenticateAs(new UsernameAndPassword("token", jwtToken));
 * RemoteWebDriver driver = RemoteWebDriver.builder()
 *     .config(config)
 *     .oneOf(new ChromeOptions())
 *     .build();
 * }</pre>
 */
@Path("/wd/hub")
@Tag(name = "WebDriver", description = "W3C WebDriver protocol proxy")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class WebDriverProxyResource {

  @Inject WebDriverProxyService proxyService;

  @Inject CallerContext callers;

  /** Creates a new browser session on the Selenium Grid via the W3C WebDriver protocol. */
  @POST
  @Path("/session")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Create a new WebDriver session", operationId = "wdCreateSession")
  public Response createSession(byte[] body) throws IOException {
    ProxyResponse resp = proxyService.createSession(body, callers.id());
    return toJaxrsResponse(resp);
  }

  /** Terminates a WebDriver session on the grid and removes it from tracking. */
  @DELETE
  @Path("/session/{sessionId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Delete a WebDriver session", operationId = "wdDeleteSession")
  public Response deleteSession(@PathParam("sessionId") String sessionId) throws IOException {
    ProxyResponse resp = proxyService.forward("DELETE", sessionId, null, null, callers.id());
    return toJaxrsResponse(resp);
  }

  /** Proxies a GET command to the session (e.g. get current URL, screenshot). */
  @GET
  @Path("/session/{sessionId}/{subPath: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(hidden = true)
  public Response getSessionCommand(
      @PathParam("sessionId") String sessionId, @PathParam("subPath") String subPath)
      throws IOException {
    ProxyResponse resp = proxyService.forward("GET", sessionId, subPath, null, callers.id());
    return toJaxrsResponse(resp);
  }

  /** Proxies a POST command to the session (e.g. navigate, execute script). */
  @POST
  @Path("/session/{sessionId}/{subPath: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(hidden = true)
  public Response postSessionCommand(
      @PathParam("sessionId") String sessionId, @PathParam("subPath") String subPath, byte[] body)
      throws IOException {
    ProxyResponse resp = proxyService.forward("POST", sessionId, subPath, body, callers.id());
    return toJaxrsResponse(resp);
  }

  /** Proxies a DELETE sub-command to the session. */
  @DELETE
  @Path("/session/{sessionId}/{subPath: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(hidden = true)
  public Response deleteSessionCommand(
      @PathParam("sessionId") String sessionId, @PathParam("subPath") String subPath)
      throws IOException {
    ProxyResponse resp = proxyService.forward("DELETE", sessionId, subPath, null, callers.id());
    return toJaxrsResponse(resp);
  }

  /** Returns session capabilities and status. */
  @GET
  @Path("/session/{sessionId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(hidden = true)
  public Response getSession(@PathParam("sessionId") String sessionId) throws IOException {
    ProxyResponse resp = proxyService.forward("GET", sessionId, null, null, callers.id());
    return toJaxrsResponse(resp);
  }

  /** Returns the grid's readiness status. */
  @GET
  @Path("/status")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get grid status", operationId = "wdGetStatus")
  public Response getStatus() throws IOException {
    ProxyResponse resp = proxyService.forwardStatus();
    return toJaxrsResponse(resp);
  }

  private static Response toJaxrsResponse(ProxyResponse proxy) {
    return Response.status(proxy.statusCode())
        .entity(proxy.body())
        .type(proxy.contentType())
        .build();
  }
}
