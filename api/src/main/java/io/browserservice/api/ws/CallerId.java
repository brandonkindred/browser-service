package io.browserservice.api.ws;

import java.util.Objects;

public final class CallerId {

  public static final int MAX_LENGTH = 128;

  private final String value;

  private CallerId(String value) {
    this.value = value;
  }

  public static CallerId parse(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("caller id is required");
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("caller id is required");
    }
    if (trimmed.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("caller id exceeds " + MAX_LENGTH + " characters");
    }
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c < 0x21 || c > 0x7E) {
        throw new IllegalArgumentException(
            "caller id contains non-printable or non-ASCII characters");
      }
    }
    return new CallerId(trimmed);
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CallerId other)) return false;
    return value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
