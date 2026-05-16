package io.browserservice.api.security;

import io.browserservice.api.config.EngineProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

  static final String BLOCKS_COUNTER_NAME = "browserservice.ssrf.blocks_total";
  static final String REASON_TAG = "reason";
  private static final byte[] GCP_METADATA_V4 = {(byte) 169, (byte) 254, (byte) 169, (byte) 254};
  private static final String GCP_METADATA_HOST = "metadata.google.internal";

  /** NAT64 well-known prefix {@code 64:ff9b::/96} — first 12 bytes of any address in the range. */
  private static final byte[] NAT64_WELL_KNOWN_PREFIX = {
    0x00, 0x64, (byte) 0xff, (byte) 0x9b, 0, 0, 0, 0, 0, 0, 0, 0
  };

  private final DnsResolver resolver;
  private final List<CidrBlock> denylist;
  private final Map<SsrfBlockReason, Counter> counters;

  /** Production constructor wired to the system DNS resolver. */
  @Inject
  public UrlSafetyValidator(EngineProperties props, MeterRegistry meters) {
    this(props, meters, DnsResolver.system());
  }

  /** Constructor for tests that need to inject a stub {@link DnsResolver}. */
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

  /**
   * Validates {@code rawUrl}. Throws {@link SsrfBlockedException} (and ticks the matching counter)
   * if the scheme is not http(s), the host is unresolvable, or any resolved address targets
   * internal infrastructure.
   */
  public void validate(String rawUrl) {
    if (rawUrl == null) {
      throw block(SsrfBlockReason.MALFORMED_URL, null);
    }
    URI uri;
    try {
      uri = URI.create(rawUrl);
    } catch (IllegalArgumentException e) {
      throw block(SsrfBlockReason.MALFORMED_URL, e);
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
    // Only consult the ambiguity check when the WHOLE host looks like a numeric IP literal —
    // matches the WHATWG URL parser's behaviour of attempting IPv4 parsing only when every label
    // is numeric. Without this gate the check would reject legitimate domains like "09.com".
    if (isAllNumericLabels(host) && hasAmbiguousNumericLabel(host)) {
      throw block(SsrfBlockReason.AMBIGUOUS_HOST_LITERAL);
    }
    InetAddress[] addresses;
    try {
      addresses = resolver.resolve(host);
    } catch (UnknownHostException e) {
      throw block(SsrfBlockReason.DNS_FAILURE, e);
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
  }

  private SsrfBlockReason inspect(InetAddress addr) {
    byte[] bytes = addr.getAddress();
    // Check the cloud-metadata literal before link-local (169.254.169.254 is in 169.254.0.0/16).
    if (bytes.length == 4 && Arrays.equals(bytes, GCP_METADATA_V4)) {
      return SsrfBlockReason.METADATA;
    }
    // NAT64 well-known prefix 64:ff9b::/96 carries an embedded IPv4 in the last 4 bytes. On
    // networks running a NAT64 gateway, 64:ff9b::a9fe:a9fe routes back to 169.254.169.254 — none
    // of Java's isXxxAddress() predicates flag the v6 form, so re-inspect the embedded v4.
    if (bytes.length == 16 && hasNat64Prefix(bytes)) {
      SsrfBlockReason embeddedReason = inspectEmbeddedV4(bytes);
      if (embeddedReason != null) {
        return embeddedReason;
      }
    }
    // IPv4-compatible IPv6 (::a.b.c.d) is deprecated by RFC 4291 but Java still resolves it and
    // does NOT auto-unwrap to Inet4Address (unlike ::ffff:/96 which it does). The JDK predicates
    // don't look inside the all-zero high 96 bits, so ::169.254.169.254 / ::10.0.0.1 / ::127.0.0.1
    // would otherwise bypass the metadata, RFC1918, and loopback checks respectively.
    if (bytes.length == 16 && hasIpv4CompatiblePrefix(bytes)) {
      SsrfBlockReason embeddedReason = inspectEmbeddedV4(bytes);
      if (embeddedReason != null) {
        return embeddedReason;
      }
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

  /**
   * Returns {@code true} when every dot-separated label in {@code host} is either decimal digits or
   * a {@code 0x}-prefixed hex literal. Trailing dot tolerated.
   */
  private static boolean isAllNumericLabels(String host) {
    int len = host.length();
    if (len == 0) {
      return false;
    }
    int end = host.charAt(len - 1) == '.' ? len - 1 : len;
    if (end == 0) {
      return false;
    }
    int labelStart = 0;
    for (int i = 0; i <= end; i++) {
      if (i == end || host.charAt(i) == '.') {
        if (!isNumericLabel(host, labelStart, i)) {
          return false;
        }
        labelStart = i + 1;
      }
    }
    return true;
  }

  private static boolean isNumericLabel(String host, int start, int end) {
    int len = end - start;
    if (len == 0) {
      return false;
    }
    if (len > 2
        && host.charAt(start) == '0'
        && (host.charAt(start + 1) == 'x' || host.charAt(start + 1) == 'X')) {
      for (int i = start + 2; i < end; i++) {
        char c = host.charAt(i);
        if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
          return false;
        }
      }
      return true;
    }
    for (int i = start; i < end; i++) {
      char c = host.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} when any label is either a multi-digit literal with a leading zero
   * (Chromium reads as octal, Java reads as decimal) or a {@code 0x}-prefixed hex literal.
   */
  private static boolean hasAmbiguousNumericLabel(String host) {
    int len = host.length();
    int end = len > 0 && host.charAt(len - 1) == '.' ? len - 1 : len;
    int labelStart = 0;
    for (int i = 0; i <= end; i++) {
      if (i == end || host.charAt(i) == '.') {
        if (i - labelStart > 1 && host.charAt(labelStart) == '0') {
          return true;
        }
        labelStart = i + 1;
      }
    }
    return false;
  }

  private static boolean hasNat64Prefix(byte[] bytes) {
    for (int i = 0; i < NAT64_WELL_KNOWN_PREFIX.length; i++) {
      if (bytes[i] != NAT64_WELL_KNOWN_PREFIX[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} when {@code bytes} is in {@code ::/96} with a non-zero embedded IPv4 — the
   * IPv4-compatible IPv6 form. Excludes {@code ::} itself (anyLocal), which the standard checks
   * below already catch.
   */
  private static boolean hasIpv4CompatiblePrefix(byte[] bytes) {
    for (int i = 0; i < 12; i++) {
      if (bytes[i] != 0) {
        return false;
      }
    }
    for (int i = 12; i < 16; i++) {
      if (bytes[i] != 0) {
        return true;
      }
    }
    return false;
  }

  private SsrfBlockReason inspectEmbeddedV4(byte[] v6Bytes) {
    byte[] embedded = Arrays.copyOfRange(v6Bytes, 12, 16);
    try {
      return inspect(InetAddress.getByAddress(embedded));
    } catch (UnknownHostException e) {
      // getByAddress only throws for non-4/16-byte arrays; we always pass 4. Unreachable.
      return null;
    }
  }

  private SsrfBlockedException block(SsrfBlockReason reason) {
    return block(reason, null);
  }

  private SsrfBlockedException block(SsrfBlockReason reason, Throwable cause) {
    counters.get(reason).increment();
    return new SsrfBlockedException(reason, cause);
  }

  private static List<CidrBlock> parseDenylist(List<String> specs) {
    if (specs == null || specs.isEmpty()) {
      return List.of();
    }
    // Fail-fast at bean construction so a typo in a security-critical denylist surfaces at
    // startup instead of silently narrowing the allow-list at runtime.
    return specs.stream().map(CidrBlock::parse).toList();
  }
}
