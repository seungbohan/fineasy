package com.fineasy.controller;

import com.fineasy.dto.ApiResponse;
import com.fineasy.dto.response.GlobalEventResponse;
import com.fineasy.service.GlobalEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/global-events")
@Tag(name = "Global Events", description = "Geopolitical and global event classification from news")
@Validated
public class GlobalEventController {

    private final GlobalEventService globalEventService;

    public GlobalEventController(GlobalEventService globalEventService) {
        this.globalEventService = globalEventService;
    }

    @GetMapping
    @Operation(summary = "Get global events",
            description = "Returns classified global events with optional type or risk level filtering")
    public ResponseEntity<ApiResponse<GlobalEventResponse>> getEvents(
            @Parameter(description = "Event type filter (GEOPOLITICAL, FISCAL, INDUSTRY, BLACK_SWAN)")
            @RequestParam(required = false) String type,
            @Parameter(description = "Risk level filter, comma-separated (e.g., HIGH,CRITICAL)")
            @RequestParam(required = false) String riskLevel,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (max 50)")
            @RequestParam(defaultValue = "20") @Min(1) int size) {

        return ResponseEntity.ok(ApiResponse.success(
                globalEventService.getEvents(type, riskLevel, page, size)));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get high-risk event alerts",
            description = "Returns HIGH/CRITICAL events from the last 24 hours")
    public ResponseEntity<ApiResponse<GlobalEventResponse>> getAlerts() {
        return ResponseEntity.ok(ApiResponse.success(globalEventService.getHighRiskEvents()));
    }
}
