package io.browserservice.api.security;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Pluggable host-to-address resolver. Production wiring delegates to {@link
 * InetAddress#getAllByName(String)}; tests inject a stub to control resolution outcomes (including
 * multi-address responses that simulate DNS rebinding).
 */
@FunctionalInterface
public interface DnsResolver {

  /** Returns all A/AAAA records bound to {@code host}, or throws if resolution fails. */
  InetAddress[] resolve(String host) throws UnknownHostException;

  /** Default production resolver that delegates to {@link InetAddress#getAllByName(String)}. */
  static DnsResolver system() {
    return InetAddress::getAllByName;
  }
}
