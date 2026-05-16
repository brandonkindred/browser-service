package io.browserservice.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.browserservice.api.config.EngineProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.Test;

class UrlSafetyValidatorTest {

  private static EngineProperties props() {
    return propsWithDenylist(List.of());
  }

  private static EngineProperties propsWithDenylist(List<String> cidrs) {
    return new EngineProperties(
        new EngineProperties.SessionProps(10, 60, 5, 5000),
        new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
        new EngineProperties.AppiumProps("", "", "", 0, 0),
        new EngineProperties.BrowserStackProps(
            false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
        new EngineProperties.WebSocketProps(
            32, 300, 64, 10000, true, 250, true, 1000, true, 2000, 50, 16777216),
        new EngineProperties.SecurityProps(cidrs));
  }

  private static UrlSafetyValidator make(SimpleMeterRegistry meters, DnsResolver resolver) {
    return new UrlSafetyValidator(props(), meters, resolver);
  }

  private static DnsResolver resolveTo(String... ips) {
    return host -> {
      InetAddress[] out = new InetAddress[ips.length];
      for (int i = 0; i < ips.length; i++) {
        out[i] = InetAddress.getByName(ips[i]);
      }
      return out;
    };
  }

  @Test
  void allowsPublicHttpsHost() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v = make(meters, resolveTo("8.8.8.8"));

    assertThat(v.validate("https://example.com/path").toString())
        .isEqualTo("https://example.com/path");
  }

  @Test
  void rejectsMetadataIpLiteral() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v = make(meters, resolveTo("169.254.169.254"));

    assertThatThrownBy(() -> v.validate("http://169.254.169.254/foo"))
        .isInstanceOf(SsrfBlockedException.class)
        .extracting("details")
        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
        .containsEntry("reason", SsrfBlockReason.METADATA.wireValue());
    assertReasonCount(meters, SsrfBlockReason.METADATA, 1);
  }

  @Test
  void rejectsMetadataHostnamePreDns() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    // Resolver should never be called for the pre-DNS metadata short-circuit.
    UrlSafetyValidator v =
        make(
            meters,
            host -> {
              throw new AssertionError("should short-circuit before DNS");
            });

    assertThatThrownBy(() -> v.validate("http://metadata.google.internal/computeMetadata/"))
        .isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.METADATA, 1);
  }

  @Test
  void rejectsLoopbackV4() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v = make(meters, resolveTo("127.0.0.1"));

    assertThatThrownBy(() -> v.validate("http://localhost/"))
        .isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.LOOPBACK, 1);
  }

  @Test
  void rejectsLoopbackV6() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v = make(meters, resolveTo("::1"));

    assertThatThrownBy(() -> v.validate("http://[::1]/")).isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.LOOPBACK, 1);
  }

  @Test
  void rejectsSiteLocalRfc1918() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v = make(meters, resolveTo("10.0.0.1"));

    assertThatThrownBy(() -> v.validate("http://10.0.0.1/"))
        .isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.SITE_LOCAL, 1);
  }

  @Test
  void rejectsLinkLocal() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v = make(meters, resolveTo("169.254.0.5"));

    assertThatThrownBy(() -> v.validate("http://169.254.0.5/"))
        .isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.LINK_LOCAL, 1);
  }

  @Test
  void rejectsMulticast() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v = make(meters, resolveTo("239.0.0.1"));

    assertThatThrownBy(() -> v.validate("http://239.0.0.1/"))
        .isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.MULTICAST, 1);
  }

  @Test
  void rejectsAnyLocal() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v = make(meters, resolveTo("0.0.0.0"));

    assertThatThrownBy(() -> v.validate("http://0.0.0.0/"))
        .isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.ANY_LOCAL, 1);
  }

  @Test
  void rejectsIpv6Ula() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v = make(meters, resolveTo("fc00::1"));

    assertThatThrownBy(() -> v.validate("http://[fc00::1]/"))
        .isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.IPV6_ULA, 1);
  }

  @Test
  void rejectsNonHttpScheme() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v =
        make(
            meters,
            host -> {
              throw new AssertionError("should not resolve");
            });

    assertThatThrownBy(() -> v.validate("file:///etc/passwd"))
        .isInstanceOf(SsrfBlockedException.class);
    assertThatThrownBy(() -> v.validate("gopher://example.com/"))
        .isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.SCHEME_DISALLOWED, 2);
  }

  @Test
  void rejectsMalformedUrl() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v =
        make(
            meters,
            host -> {
              throw new AssertionError("should not resolve");
            });

    assertThatThrownBy(() -> v.validate("not a url")).isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.MALFORMED_URL, 1);
  }

  @Test
  void rejectsDnsFailure() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v =
        make(
            meters,
            host -> {
              throw new UnknownHostException(host);
            });

    assertThatThrownBy(() -> v.validate("https://nope.example/"))
        .isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.DNS_FAILURE, 1);
  }

  @Test
  void rejectsWhenAnyAddressInMultipleAddressResponseIsUnsafe() {
    // Multi-A response: one resolve() returns both a public and a private address.
    // The validator must inspect every entry and reject on the offender. (Note: this is not
    // DNS rebinding proper — that is a TOCTOU between two separate resolutions, accepted as a
    // documented residual risk closed at the egress proxy.)
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v = make(meters, resolveTo("8.8.8.8", "10.1.2.3"));

    assertThatThrownBy(() -> v.validate("http://multi-a.example/"))
        .isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.SITE_LOCAL, 1);
  }

  @Test
  void rejectsConfiguredDenylistCidr() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    UrlSafetyValidator v =
        new UrlSafetyValidator(
            propsWithDenylist(List.of("100.64.0.0/10")), meters, resolveTo("100.64.0.5"));

    assertThatThrownBy(() -> v.validate("http://cgnat.example/"))
        .isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.DENYLIST_CIDR, 1);
  }

  @Test
  void invalidDenylistEntryIsIgnoredAtConstruction() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    // Garbage CIDR should not crash construction; valid CIDRs in the same list still apply.
    UrlSafetyValidator v =
        new UrlSafetyValidator(
            propsWithDenylist(List.of("not-a-cidr", "100.64.0.0/10")),
            meters,
            resolveTo("100.64.0.5"));

    assertThatThrownBy(() -> v.validate("http://cgnat.example/"))
        .isInstanceOf(SsrfBlockedException.class);
    assertReasonCount(meters, SsrfBlockReason.DENYLIST_CIDR, 1);
  }

  private static void assertReasonCount(
      SimpleMeterRegistry meters, SsrfBlockReason reason, int expected) {
    double count =
        meters
            .counter(
                UrlSafetyValidator.BLOCKS_COUNTER_NAME,
                UrlSafetyValidator.REASON_TAG,
                reason.wireValue())
            .count();
    assertThat(count).isEqualTo(expected);
  }
}
