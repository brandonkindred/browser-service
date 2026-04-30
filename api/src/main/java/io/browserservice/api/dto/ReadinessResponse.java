package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Readiness probe result including reachability of each upstream hub/server.")
public record ReadinessResponse(
        @Schema(description = "Aggregate readiness status", allowableValues = {"ready", "degraded"})
        String status,
        @Schema(description = "Reachability of each configured Selenium Grid URL")
        List<HubStatus> seleniumHubs,
        @Schema(description = "Reachability of each configured Appium server URL")
        List<HubStatus> appiumServers) {
}
