package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a navigation attempt.")
public enum NavigateStatus {
    LOADED,
    TIMEOUT,
    ERROR
}
