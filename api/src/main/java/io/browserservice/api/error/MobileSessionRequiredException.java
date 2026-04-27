package io.browserservice.api.error;

import org.springframework.http.HttpStatus;

public class MobileSessionRequiredException extends ApiException {
  public MobileSessionRequiredException() {
    super(
        "mobile_session_required",
        HttpStatus.CONFLICT,
        "this operation is only valid for mobile sessions");
  }
}
