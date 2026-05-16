package io.browserservice.api.security;

/**
 * Reason a URL was rejected by {@link UrlSafetyValidator}. The {@link #wireValue()} is the
 * snake-case tag attached to the {@code browserservice.ssrf.blocks_total} counter and surfaced in
 * the {@code details.reason} field of the HTTP 400 response.
 */
public enum SsrfBlockReason {
  MALFORMED_URL("malformed_url"),
  SCHEME_DISALLOWED("scheme_disallowed"),
  DNS_FAILURE("dns_failure"),
  LOOPBACK("loopback"),
  LINK_LOCAL("link_local"),
  SITE_LOCAL("site_local"),
  MULTICAST("multicast"),
  ANY_LOCAL("any_local"),
  METADATA("metadata"),
  IPV6_ULA("ipv6_ula"),
  DENYLIST_CIDR("denylist_cidr");

  private final String wireValue;

  SsrfBlockReason(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
