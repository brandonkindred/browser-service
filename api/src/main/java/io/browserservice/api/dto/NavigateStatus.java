package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Result of a navigation attempt.")
public enum NavigateStatus {
  LOADED,
  TIMEOUT,
  ERROR
}
