package io.browserservice.api.ws;

import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Builds a {@link JWTParser} from the {@code mp.jwt.verify.*} configuration without bringing in the
 * {@code quarkus-smallrye-jwt} extension. That extension registers a second HTTP Bearer
 * authentication mechanism alongside {@code quarkus-oidc}, which the Quarkus security docs warn
 * cannot be combined (both try to verify the same {@code Authorization: Bearer} token). We only
 * need the parser bean for {@link CallerIdUpgradeCheck}'s manual WS-subprotocol validation, so we
 * wire it up by hand instead.
 */
@ApplicationScoped
public class JwtParserProducer {

  private final String issuer;
  private final String audiences;
  private final String publicKeyLocation;

  /** Reads the {@code mp.jwt.verify.*} config keys; matches the public defaults from #89. */
  public JwtParserProducer(
      @ConfigProperty(name = "mp.jwt.verify.issuer") String issuer,
      @ConfigProperty(name = "mp.jwt.verify.audiences") String audiences,
      @ConfigProperty(name = "mp.jwt.verify.publickey.location") String publicKeyLocation) {
    this.issuer = issuer;
    this.audiences = audiences;
    this.publicKeyLocation = publicKeyLocation;
  }

  /**
   * Produces the application-scoped {@link JWTParser} used by the WebSocket upgrade check. Keys
   * fetched from the JWKS URL are cached inside {@link DefaultJWTParser} for the lifetime of the
   * bean so repeated handshakes don't refetch.
   */
  @Produces
  @ApplicationScoped
  public JWTParser jwtParser() {
    JWTAuthContextInfo info = new JWTAuthContextInfo();
    info.setIssuedBy(issuer);
    info.setExpectedAudience(parseAudiences(audiences));
    info.setPublicKeyLocation(publicKeyLocation);
    return new DefaultJWTParser(info);
  }

  private static Set<String> parseAudiences(String raw) {
    if (raw == null || raw.isBlank()) {
      return Collections.emptySet();
    }
    Set<String> result = new HashSet<>();
    for (String s : raw.split(",")) {
      String trimmed = s.trim();
      if (!trimmed.isEmpty()) {
        result.add(trimmed);
      }
    }
    return result;
  }
}
