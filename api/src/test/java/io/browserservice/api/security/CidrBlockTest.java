package io.browserservice.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;

class CidrBlockTest {

  @Test
  void v4BlockMatchesAddressesInsideRange() throws Exception {
    CidrBlock block = CidrBlock.parse("10.0.0.0/8");
    assertThat(block.contains(InetAddress.getByName("10.0.0.0"))).isTrue();
    assertThat(block.contains(InetAddress.getByName("10.255.255.255"))).isTrue();
    assertThat(block.contains(InetAddress.getByName("10.1.2.3"))).isTrue();
  }

  @Test
  void v4BlockRejectsAddressesOutsideRange() throws Exception {
    CidrBlock block = CidrBlock.parse("10.0.0.0/8");
    assertThat(block.contains(InetAddress.getByName("11.0.0.0"))).isFalse();
    assertThat(block.contains(InetAddress.getByName("9.255.255.255"))).isFalse();
    assertThat(block.contains(InetAddress.getByName("192.168.0.1"))).isFalse();
  }

  @Test
  void v4SubByteBoundary() throws Exception {
    CidrBlock block = CidrBlock.parse("100.64.0.0/10");
    assertThat(block.contains(InetAddress.getByName("100.64.0.1"))).isTrue();
    assertThat(block.contains(InetAddress.getByName("100.127.255.255"))).isTrue();
    assertThat(block.contains(InetAddress.getByName("100.128.0.0"))).isFalse();
    assertThat(block.contains(InetAddress.getByName("100.63.255.255"))).isFalse();
  }

  @Test
  void v6BlockMatchesUla() throws Exception {
    CidrBlock block = CidrBlock.parse("fc00::/7");
    assertThat(block.contains(InetAddress.getByName("fc00::1"))).isTrue();
    assertThat(block.contains(InetAddress.getByName("fd12::abcd"))).isTrue();
    assertThat(block.contains(InetAddress.getByName("fe00::1"))).isFalse();
  }

  @Test
  void v4PrefixDoesNotMatchV6Address() throws Exception {
    CidrBlock block = CidrBlock.parse("10.0.0.0/8");
    assertThat(block.contains(InetAddress.getByName("::1"))).isFalse();
  }

  @Test
  void v6PrefixDoesNotMatchV4Address() throws Exception {
    CidrBlock block = CidrBlock.parse("fc00::/7");
    assertThat(block.contains(InetAddress.getByName("10.0.0.1"))).isFalse();
  }

  @Test
  void missingSlashThrows() {
    assertThatThrownBy(() -> CidrBlock.parse("10.0.0.0"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void nonNumericPrefixThrows() {
    assertThatThrownBy(() -> CidrBlock.parse("10.0.0.0/abc"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void prefixOutOfRangeThrows() {
    assertThatThrownBy(() -> CidrBlock.parse("10.0.0.0/33"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> CidrBlock.parse("fc00::/129"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
