package com.fineasy.controller;

import com.fineasy.dto.response.AnalysisReportResponse;
import com.fineasy.dto.response.PredictionResponse;
import com.fineasy.service.AnalysisService;
import com.fineasy.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@Tag(name = "AI Analysis", description = "AI-powered stock analysis and prediction")
@Validated
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/{stockCode}/report")
    @Operation(summary = "Get AI analysis report",
            description = "Generate or retrieve cached AI analysis report for the given stock")
    public ResponseEntity<ApiResponse<AnalysisReportResponse>> getReport(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(
                analysisService.getReport(stockCode)));
    }

    @GetMapping("/{stockCode}/prediction")
    @Operation(summary = "Get stock price prediction",
            description = "AI-powered stock price direction prediction. Supports 1D (one trading day) and 1W (one week) periods.")
    public ResponseEntity<ApiResponse<PredictionResponse>> getPrediction(
            @PathVariable @NotBlank String stockCode,
            @RequestParam(defaultValue = "1D")
            @Pattern(regexp = "1D|1W", message = "period must be '1D' or '1W'")
            @Parameter(description = "Prediction period: '1D' for one trading day, '1W' for one week")
            String period) {
        return ResponseEntity.ok(ApiResponse.success(
                analysisService.getPrediction(stockCode, period)));
    }

    @GetMapping("/{stockCode}/history")
    @Operation(summary = "Get analysis history for a stock",
            description = "Retrieve past AI analysis reports (up to 30, most recent first)")
    public ResponseEntity<ApiResponse<List<AnalysisReportResponse>>> getHistory(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(
                analysisService.getHistory(stockCode)));
    }
}
