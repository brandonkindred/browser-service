package io.browserservice.api.security;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IPv4 or IPv6 CIDR block, parsed once from configuration and matched against resolved addresses. A
 * v4 block does not match v6 addresses (and vice versa) — callers wanting both families must
 * configure two entries.
 */
record CidrBlock(byte[] network, int prefixLen, boolean isV6) {

  static CidrBlock parse(String spec) {
    if (spec == null) {
      throw new IllegalArgumentException("null CIDR");
    }
    int slash = spec.indexOf('/');
    if (slash < 0) {
      throw new IllegalArgumentException("missing '/' in CIDR: " + spec);
    }
    String addr = spec.substring(0, slash).trim();
    if (addr.isEmpty()) {
      // Reject explicitly — InetAddress.getByName("") silently returns the loopback address,
      // which would otherwise produce a 127.0.0.0/n block the operator never asked for.
      throw new IllegalArgumentException("missing address in CIDR: " + spec);
    }
    int prefix;
    try {
      prefix = Integer.parseInt(spec.substring(slash + 1).trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("invalid prefix in CIDR: " + spec, e);
    }
    InetAddress parsed;
    try {
      parsed = InetAddress.getByName(addr);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("invalid address in CIDR: " + spec, e);
    }
    byte[] bytes = parsed.getAddress();
    boolean v6 = bytes.length == 16;
    int max = v6 ? 128 : 32;
    if (prefix < 0 || prefix > max) {
      throw new IllegalArgumentException("prefix out of range in CIDR: " + spec);
    }
    return new CidrBlock(applyMask(bytes, prefix), prefix, v6);
  }

  boolean contains(InetAddress addr) {
    byte[] bytes = addr.getAddress();
    boolean addrV6 = bytes.length == 16;
    if (addrV6 != isV6) {
      return false;
    }
    int fullBytes = prefixLen / 8;
    int remainingBits = prefixLen % 8;
    for (int i = 0; i < fullBytes; i++) {
      if (bytes[i] != network[i]) {
        return false;
      }
    }
    if (remainingBits == 0) {
      return true;
    }
    int mask = (0xFF << (8 - remainingBits)) & 0xFF;
    return (bytes[fullBytes] & mask) == (network[fullBytes] & mask);
  }

  private static byte[] applyMask(byte[] addr, int prefix) {
    byte[] masked = addr.clone();
    int fullBytes = prefix / 8;
    int remainingBits = prefix % 8;
    for (int i = fullBytes + (remainingBits == 0 ? 0 : 1); i < masked.length; i++) {
      masked[i] = 0;
    }
    if (remainingBits != 0 && fullBytes < masked.length) {
      int mask = (0xFF << (8 - remainingBits)) & 0xFF;
      masked[fullBytes] = (byte) (masked[fullBytes] & mask);
    }
    return masked;
  }
}
