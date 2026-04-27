package io.browserservice.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionsTest {

  @Test
  void sessionNotFoundCarriesId() {
    UUID id = UUID.randomUUID();
    SessionNotFoundException ex = new SessionNotFoundException(id);

    assertThat(ex.getCode()).isEqualTo("session_not_found");
    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ex.getDetails()).containsEntry("session_id", id.toString());
    assertThat(ex.getMessage()).contains(id.toString());
  }

  @Test
  void sessionCapExceededReportsMax() {
    SessionCapExceededException ex = new SessionCapExceededException(20);
    assertThat(ex.getCode()).isEqualTo("session_cap_exceeded");
    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(ex.getDetails()).containsEntry("max_concurrent", 20);
  }

  @Test
  void sessionBusyCarriesId() {
    UUID id = UUID.randomUUID();
    SessionBusyException ex = new SessionBusyException(id);
    assertThat(ex.getCode()).isEqualTo("session_busy");
    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(ex.getDetails()).containsEntry("session_id", id.toString());
  }

  @Test
  void elementHandleNotFoundCarriesHandle() {
    ElementHandleNotFoundException ex = new ElementHandleNotFoundException("el_42");
    assertThat(ex.getCode()).isEqualTo("element_handle_not_found");
    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ex.getDetails()).containsEntry("element_handle", "el_42");
  }

  @Test
  void captureNotFoundAndExpiredMapCorrectly() {
    UUID id = UUID.randomUUID();
    CaptureNotFoundException nf = new CaptureNotFoundException(id);
    CaptureExpiredException expired = new CaptureExpiredException(id);

    assertThat(nf.getCode()).isEqualTo("capture_not_found");
    assertThat(nf.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(expired.getCode()).isEqualTo("capture_expired");
    assertThat(expired.getHttpStatus()).isEqualTo(HttpStatus.GONE);
  }

  @Test
  void mobileVsDesktopRequiredMapToConflict() {
    assertThat(new MobileSessionRequiredException().getCode()).isEqualTo("mobile_session_required");
    assertThat(new MobileSessionRequiredException().getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(new DesktopSessionRequiredException().getCode())
        .isEqualTo("desktop_session_required");
    assertThat(new DesktopSessionRequiredException().getHttpStatus())
        .isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void upstreamUnavailableSupportsCause() {
    Throwable cause = new RuntimeException("boom");
    UpstreamUnavailableException withCause = new UpstreamUnavailableException("bad", cause);
    UpstreamUnavailableException bare = new UpstreamUnavailableException("bad");

    assertThat(withCause.getCode()).isEqualTo("upstream_unavailable");
    assertThat(withCause.getCause()).isSameAs(cause);
    assertThat(bare.getCause()).isNull();
  }

  @Test
  void validationFailedHasDetailsOverload() {
    ValidationFailedException msgOnly = new ValidationFailedException("bad");
    ValidationFailedException withDetails =
        new ValidationFailedException("bad", java.util.Map.of("field", "x"));

    assertThat(msgOnly.getDetails()).isNull();
    assertThat(withDetails.getDetails()).containsEntry("field", "x");
    assertThat(withDetails.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
