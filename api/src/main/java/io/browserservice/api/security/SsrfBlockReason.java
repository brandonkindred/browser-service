package io.browserservice.api.security;

/**
 * Reason a URL was rejected by {@link UrlSafetyValidator}. The {@link #wireValue()} is the
 * snake-case tag attached to the {@code browserservice.ssrf.blocks_total} counter and surfaced in
 * the {@code details.reason} field of the HTTP 400 response.
 */
public enum SsrfBlockReason {
  MALFORMED_URL("malformed_url"),
  SCHEME_DISALLOWED("scheme_disallowed"),
  AMBIGUOUS_HOST_LITERAL("ambiguous_host_literal"),
  DNS_FAILURE("dns_failure"),
  LOOPBACK("loopback"),
  LINK_LOCAL("link_local"),
  SITE_LOCAL("site_local"),
  MULTICAST("multicast"),
  ANY_LOCAL("any_local"),
  METADATA("metadata"),
  IPV6_ULA("ipv6_ula"),
  DENYLIST_CIDR("denylist_cidr");

  private final String tag;

  SsrfBlockReason(String tag) {
    this.tag = tag;
  }

  /** Snake-case tag value surfaced in the response and on the metric. */
  public String wireValue() {
    return tag;
  }
}
