package com.fineasy.controller;

import com.fineasy.dto.ApiResponse;
import com.fineasy.dto.response.EtfPresetResponse;
import com.fineasy.service.EtfPresetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calculator")
@Tag(name = "Calculator", description = "Investment calculator tools")
public class CalculatorController {

    private final EtfPresetService etfPresetService;

    public CalculatorController(EtfPresetService etfPresetService) {
        this.etfPresetService = etfPresetService;
    }

    @GetMapping("/etf-presets")
    @Operation(summary = "Get ETF presets",
            description = "Returns predefined ETF presets with annual return rates for investment simulation")
    public ResponseEntity<ApiResponse<EtfPresetResponse>> getEtfPresets() {
        return ResponseEntity.ok(ApiResponse.success(etfPresetService.getAllPresets()));
    }
}
