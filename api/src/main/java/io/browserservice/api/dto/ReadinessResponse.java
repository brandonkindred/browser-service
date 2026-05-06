package io.browserservice.api.dto;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Readiness probe result including reachability of each upstream hub/server.")
public record ReadinessResponse(
    @Schema(
            description = "Aggregate readiness status",
            enumeration = {"ready", "degraded"})
        String status,
    @Schema(description = "Reachability of each configured Selenium Grid URL")
        List<HubStatus> seleniumHubs,
    @Schema(description = "Reachability of each configured Appium server URL")
        List<HubStatus> appiumServers) {}
