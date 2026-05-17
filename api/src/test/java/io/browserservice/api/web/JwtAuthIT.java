package io.browserservice.api.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.browserservice.api.testsupport.TestTokens;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Acceptance tests for the bearer-token auth contract added in issue #89. Mirrors the verification
 * list in the issue body:
 *
 * <ul>
 *   <li>Unauthenticated request → 401
 *   <li>Tampered JWT → 401
 *   <li>Expired token → 401
 *   <li>Wrong issuer → 401
 *   <li>Wrong audience → 401
 *   <li>Missing {@code tenant_id} claim → 401 with code {@code missing_tenant_claim}
 *   <li>Valid token → request reaches the controller layer
 *   <li>Public ops endpoints reachable without a token
 * </ul>
 */
@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
@TestProfile(JwtAuthIT.AuthTestProfile.class)
class JwtAuthIT {

  @Test
  void unauthenticatedRequestIsRejected() {
    given().when().get("/v1/sessions").then().statusCode(401);
  }

  @Test
  void tamperedTokenIsRejected() {
    given()
        .header("Authorization", "Bearer " + TestTokens.tampered("alice"))
        .when()
        .get("/v1/sessions")
        .then()
        .statusCode(401);
  }

  @Test
  void expiredTokenIsRejected() {
    given()
        .header("Authorization", "Bearer " + TestTokens.expired("alice"))
        .when()
        .get("/v1/sessions")
        .then()
        .statusCode(401);
  }

  @Test
  void wrongIssuerIsRejected() {
    given()
        .header("Authorization", "Bearer " + TestTokens.wrongIssuer("alice"))
        .when()
        .get("/v1/sessions")
        .then()
        .statusCode(401);
  }

  @Test
  void wrongAudienceIsRejected() {
    given()
        .header("Authorization", "Bearer " + TestTokens.wrongAudience("alice"))
        .when()
        .get("/v1/sessions")
        .then()
        .statusCode(401);
  }

  @Test
  void missingTenantClaimIsRejectedWithSpecificCode() {
    given()
        .header("Authorization", "Bearer " + TestTokens.missingTenant("alice"))
        .when()
        .get("/v1/sessions")
        .then()
        .statusCode(401)
        .body("error.code", equalTo("missing_tenant_claim"));
  }

  @Test
  void validTokenReachesTheController() {
    // GET /v1/sessions returns 200 with an empty session list when no sessions exist for the
    // caller — the key thing this test proves is that the auth layer accepted the token and
    // let the request through to the controller.
    given()
        .header("Authorization", "Bearer " + TestTokens.mint("alice"))
        .when()
        .get("/v1/sessions")
        .then()
        .statusCode(200);
  }

  @Test
  void healthzReachableWithoutToken() {
    given().when().get("/healthz").then().statusCode(200);
  }

  @Test
  void readyzReachableWithoutToken() {
    // /readyz returns 200 or 503 depending on upstream readiness — both are non-401, which is
    // what we're proving: the auth layer doesn't gate it.
    given()
        .when()
        .get("/readyz")
        .then()
        .statusCode(org.hamcrest.Matchers.anyOf(equalTo(200), equalTo(503)));
  }

  @Test
  void metricsReachableWithoutToken() {
    // The point is that the auth layer doesn't return 401 — the exact status (200 with a body or
    // 404 if micrometer isn't bound at this exact path in the test profile) is irrelevant here.
    given().when().get("/metrics").then().statusCode(org.hamcrest.Matchers.not(equalTo(401)));
  }

  public static class AuthTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      // H2 in-memory: keep this test focused on the auth pipeline, not on DB infrastructure.
      // OidcWiremockTestResource injects quarkus.oidc.auth-server-url at runtime.
      return Map.ofEntries(
          Map.entry("quarkus.datasource.devservices.enabled", "false"),
          Map.entry("quarkus.datasource.db-kind", "h2"),
          Map.entry(
              "quarkus.datasource.jdbc.url",
              "jdbc:h2:mem:jwt-auth-it;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"),
          Map.entry("quarkus.datasource.username", "sa"),
          Map.entry("quarkus.datasource.password", ""),
          Map.entry("quarkus.flyway.migrate-at-start", "false"),
          Map.entry("quarkus.hibernate-orm.database.generation", "drop-and-create"),
          Map.entry("browserservice.selenium.urls", "http://localhost:4444/wd/hub"),
          Map.entry("smallrye.jwt.sign.key.location", "/privateKey.jwk"));
    }
  }
}
