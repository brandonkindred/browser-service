package io.browserservice.api.security;

import io.browserservice.api.config.EngineProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Application-layer SSRF guard. Parses the URL, resolves the host, and rejects any address that
 * targets internal infrastructure (loopback, link-local, site-local, multicast, cloud metadata,
 * IPv6 ULA) or that matches a configured denylist CIDR. The returned URI is unchanged — we do not
 * rewrite to an IP literal because the WebDriver-driven browser performs its own DNS lookup and an
 * IP-literal rewrite would break TLS SNI/cert verification for HTTPS targets. The residual TOCTOU
 * window is closed at the network egress layer by the Squid proxy (tracked under issue N3).
 *
 * <p>Every rejection increments the {@code browserservice.ssrf.blocks_total} counter tagged by
 * {@link SsrfBlockReason}; the Prometheus exporter exposes this as {@code
 * browserservice_ssrf_blocks_total{reason="..."}}.
 */
@Component
public class UrlSafetyValidator {

  private static final Logger log = LoggerFactory.getLogger(UrlSafetyValidator.class);

  static final String BLOCKS_COUNTER_NAME = "browserservice.ssrf.blocks_total";
  static final String REASON_TAG = "reason";
  private static final byte[] GCP_METADATA_V4 = {(byte) 169, (byte) 254, (byte) 169, (byte) 254};
  private static final String GCP_METADATA_HOST = "metadata.google.internal";

  private final DnsResolver resolver;
  private final List<CidrBlock> denylist;
  private final Map<SsrfBlockReason, Counter> counters;

  @Inject
  public UrlSafetyValidator(EngineProperties props, MeterRegistry meters) {
    this(props, meters, DnsResolver.system());
  }

  public UrlSafetyValidator(EngineProperties props, MeterRegistry meters, DnsResolver resolver) {
    this.resolver = resolver;
    this.denylist = parseDenylist(props.security().ssrfDenylistCidrs());
    this.counters = new EnumMap<>(SsrfBlockReason.class);
    for (SsrfBlockReason reason : SsrfBlockReason.values()) {
      counters.put(
          reason,
          Counter.builder(BLOCKS_COUNTER_NAME)
              .description("URLs blocked by the SSRF guard, tagged by rejection reason")
              .tag(REASON_TAG, reason.wireValue())
              .register(meters));
    }
  }

  public URI validate(String rawUrl) {
    URI uri;
    try {
      uri = URI.create(rawUrl);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw block(SsrfBlockReason.MALFORMED_URL);
    }
    String scheme = uri.getScheme();
    if (scheme == null) {
      throw block(SsrfBlockReason.MALFORMED_URL);
    }
    String lowerScheme = scheme.toLowerCase(Locale.ROOT);
    if (!"http".equals(lowerScheme) && !"https".equals(lowerScheme)) {
      throw block(SsrfBlockReason.SCHEME_DISALLOWED);
    }
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw block(SsrfBlockReason.MALFORMED_URL);
    }
    if (GCP_METADATA_HOST.equalsIgnoreCase(host)) {
      throw block(SsrfBlockReason.METADATA);
    }
    InetAddress[] addresses;
    try {
      addresses = resolver.resolve(host);
    } catch (UnknownHostException e) {
      throw block(SsrfBlockReason.DNS_FAILURE);
    }
    if (addresses == null || addresses.length == 0) {
      throw block(SsrfBlockReason.DNS_FAILURE);
    }
    for (InetAddress addr : addresses) {
      SsrfBlockReason reason = inspect(addr);
      if (reason != null) {
        throw block(reason);
      }
    }
    return uri;
  }

  private SsrfBlockReason inspect(InetAddress addr) {
    byte[] bytes = addr.getAddress();
    // Check the cloud-metadata literal before link-local (169.254.169.254 is in 169.254.0.0/16).
    if (bytes.length == 4 && java.util.Arrays.equals(bytes, GCP_METADATA_V4)) {
      return SsrfBlockReason.METADATA;
    }
    if (addr.isAnyLocalAddress()) {
      return SsrfBlockReason.ANY_LOCAL;
    }
    if (addr.isLoopbackAddress()) {
      return SsrfBlockReason.LOOPBACK;
    }
    if (addr.isLinkLocalAddress()) {
      return SsrfBlockReason.LINK_LOCAL;
    }
    if (addr.isSiteLocalAddress()) {
      return SsrfBlockReason.SITE_LOCAL;
    }
    if (addr.isMulticastAddress()) {
      return SsrfBlockReason.MULTICAST;
    }
    if (bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC) {
      return SsrfBlockReason.IPV6_ULA;
    }
    for (CidrBlock block : denylist) {
      if (block.contains(addr)) {
        return SsrfBlockReason.DENYLIST_CIDR;
      }
    }
    return null;
  }

  private SsrfBlockedException block(SsrfBlockReason reason) {
    counters.get(reason).increment();
    return new SsrfBlockedException(reason);
  }

  private static List<CidrBlock> parseDenylist(List<String> specs) {
    if (specs == null || specs.isEmpty()) {
      return List.of();
    }
    return specs.stream()
        .map(
            spec -> {
              try {
                return CidrBlock.parse(spec);
              } catch (IllegalArgumentException e) {
                log.warn("ignoring invalid SSRF denylist CIDR: {}", spec, e);
                return null;
              }
            })
        .filter(java.util.Objects::nonNull)
        .toList();
  }
}
