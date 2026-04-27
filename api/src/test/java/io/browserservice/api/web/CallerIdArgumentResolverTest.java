package io.browserservice.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.browserservice.api.error.CallerUnidentifiedException;
import io.browserservice.api.session.CallerId;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

class CallerIdArgumentResolverTest {

  private final CallerIdArgumentResolver resolver = new CallerIdArgumentResolver();

  @Test
  void supportsCallerIdParameterOnly() throws NoSuchMethodException {
    assertThat(
            resolver.supportsParameter(
                new org.springframework.core.MethodParameter(
                    Sample.class.getDeclaredMethod("withCaller", CallerId.class), 0)))
        .isTrue();
    assertThat(
            resolver.supportsParameter(
                new org.springframework.core.MethodParameter(
                    Sample.class.getDeclaredMethod("withString", String.class), 0)))
        .isFalse();
  }

  @Test
  void missingHeaderThrowsCallerUnidentified() {
    NativeWebRequest req = wrap(new MockHttpServletRequest());

    assertThatThrownBy(() -> resolver.resolveArgument(null, null, req, null))
        .isInstanceOf(CallerUnidentifiedException.class)
        .extracting("code")
        .isEqualTo("caller_unidentified");
  }

  @Test
  void blankHeaderThrowsCallerUnidentified() {
    MockHttpServletRequest http = new MockHttpServletRequest();
    http.addHeader(CallerIdArgumentResolver.CALLER_HEADER, "   ");

    assertThatThrownBy(() -> resolver.resolveArgument(null, null, wrap(http), null))
        .isInstanceOf(CallerUnidentifiedException.class);
  }

  @Test
  void oversizeHeaderThrowsCallerUnidentifiedWithCause() {
    MockHttpServletRequest http = new MockHttpServletRequest();
    http.addHeader(CallerIdArgumentResolver.CALLER_HEADER, "a".repeat(CallerId.MAX_LENGTH + 1));

    assertThatThrownBy(() -> resolver.resolveArgument(null, null, wrap(http), null))
        .isInstanceOf(CallerUnidentifiedException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void internalWhitespaceThrowsCallerUnidentified() {
    MockHttpServletRequest http = new MockHttpServletRequest();
    http.addHeader(CallerIdArgumentResolver.CALLER_HEADER, "alice smith");

    assertThatThrownBy(() -> resolver.resolveArgument(null, null, wrap(http), null))
        .isInstanceOf(CallerUnidentifiedException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void nonAsciiThrowsCallerUnidentified() {
    MockHttpServletRequest http = new MockHttpServletRequest();
    http.addHeader(CallerIdArgumentResolver.CALLER_HEADER, "alicé");

    assertThatThrownBy(() -> resolver.resolveArgument(null, null, wrap(http), null))
        .isInstanceOf(CallerUnidentifiedException.class);
  }

  @Test
  void validHeaderReturnsCallerId() {
    MockHttpServletRequest http = new MockHttpServletRequest();
    http.addHeader(CallerIdArgumentResolver.CALLER_HEADER, "alice");

    Object result = resolver.resolveArgument(null, null, wrap(http), null);

    assertThat(result).isInstanceOf(CallerId.class);
    assertThat(((CallerId) result).value()).isEqualTo("alice");
  }

  @Test
  void surroundingWhitespaceIsTrimmed() {
    MockHttpServletRequest http = new MockHttpServletRequest();
    http.addHeader(CallerIdArgumentResolver.CALLER_HEADER, "  alice  ");

    Object result = resolver.resolveArgument(null, null, wrap(http), null);

    assertThat(((CallerId) result).value()).isEqualTo("alice");
  }

  private static NativeWebRequest wrap(MockHttpServletRequest req) {
    return new ServletWebRequest(req);
  }

  @SuppressWarnings("unused")
  private static final class Sample {
    void withCaller(CallerId caller) {}

    void withString(String s) {}
  }
}
