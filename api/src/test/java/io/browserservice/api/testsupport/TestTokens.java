package io.browserservice.api.testsupport;

import io.smallrye.jwt.build.Jwt;
import java.time.Duration;
import java.time.Instant;

/**
 * Helpers for minting JWTs that the production code's quarkus-oidc filter (and the WebSocket
 * upgrade check's {@code JWTParser}) accept as valid bearer tokens during {@code @QuarkusTest}
 * runs.
 *
 * <p>Tokens are signed with the private key bundled in {@code
 * io.quarkus:quarkus-test-oidc-server}'s jar at {@code /privateKey.jwk}. The public half is
 * published by {@link io.quarkus.test.oidc.server.OidcWiremockTestResource}'s JWKS endpoint, so
 * tokens minted here verify cleanly against the running OIDC mock. The issuer is read from the live
 * config (overridden per-run by the test resource); the audience matches the default the test
 * resource bakes in.
 */
public final class TestTokens {

  public static final String DEFAULT_TENANT = "test-tenant";

  /** Static issuer baked into tokens minted by OidcWiremockTestResource. */
  public static final String DEFAULT_ISSUER = "https://server.example.com";

  public static final String DEFAULT_AUDIENCE = "https://server.example.com";
  static final String SIGN_KEY_LOCATION = "/privateKey.jwk";

  private TestTokens() {}

  /** Mints a token whose {@code sub} is {@code subject} and {@code tenant_id} is the default. */
  public static String mint(String subject) {
    return mint(DEFAULT_TENANT, subject);
  }

  /** Mints a token with explicit tenant and subject claims; otherwise valid for ~15min. */
  public static String mint(String tenantId, String subject) {
    return baseToken(tenantId, subject).sign(SIGN_KEY_LOCATION);
  }

  /** Mints a token whose lifetime has already elapsed. */
  public static String expired(String subject) {
    return baseToken(DEFAULT_TENANT, subject)
        .issuedAt(Instant.now().minus(Duration.ofHours(2)))
        .expiresAt(Instant.now().minus(Duration.ofHours(1)))
        .sign(SIGN_KEY_LOCATION);
  }

  /** Mints a token signed by the right key but addressed to a different audience. */
  public static String wrongAudience(String subject) {
    return Jwt.subject(subject)
        .claim("tenant_id", DEFAULT_TENANT)
        .issuer(DEFAULT_ISSUER)
        .audience("https://attacker.example.com")
        .sign(SIGN_KEY_LOCATION);
  }

  /** Mints a token signed by the right key but claiming a different issuer. */
  public static String wrongIssuer(String subject) {
    return Jwt.subject(subject)
        .claim("tenant_id", DEFAULT_TENANT)
        .issuer("https://attacker.example.com")
        .audience(DEFAULT_AUDIENCE)
        .sign(SIGN_KEY_LOCATION);
  }

  /** Mints a token that omits the {@code tenant_id} claim required by {@code CallerContext}. */
  public static String missingTenant(String subject) {
    return Jwt.subject(subject)
        .issuer(DEFAULT_ISSUER)
        .audience(DEFAULT_AUDIENCE)
        .sign(SIGN_KEY_LOCATION);
  }

  /** Mints an otherwise-valid token whose {@code tenant_id} is a number instead of a string. */
  public static String numericTenant(String subject) {
    return Jwt.subject(subject)
        .claim("tenant_id", 12345)
        .issuer(DEFAULT_ISSUER)
        .audience(DEFAULT_AUDIENCE)
        .sign(SIGN_KEY_LOCATION);
  }

  /** Replaces the signature segment of a valid token with garbage so verification fails. */
  public static String tampered(String subject) {
    String valid = mint(subject);
    int lastDot = valid.lastIndexOf('.');
    return valid.substring(0, lastDot + 1)
        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
  }

  private static io.smallrye.jwt.build.JwtClaimsBuilder baseToken(String tenantId, String subject) {
    return Jwt.subject(subject)
        .claim("tenant_id", tenantId)
        .issuer(DEFAULT_ISSUER)
        .audience(DEFAULT_AUDIENCE);
  }
}
