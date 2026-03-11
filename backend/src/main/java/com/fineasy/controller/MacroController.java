package com.fineasy.controller;

import com.fineasy.dto.response.MacroIndicatorResponse;
import com.fineasy.dto.response.MarketRiskResponse;
import com.fineasy.service.MacroService;
import com.fineasy.service.MarketRiskService;
import com.fineasy.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/macro")
@Tag(name = "Macroeconomic", description = "Macroeconomic indicators")
@Validated
public class MacroController {

    private final MacroService macroService;
    private final MarketRiskService marketRiskService;

    public MacroController(MacroService macroService, MarketRiskService marketRiskService) {
        this.macroService = macroService;
        this.marketRiskService = marketRiskService;
    }

    @GetMapping("/indicators")
    @Operation(summary = "Get latest macroeconomic indicators")
    public ResponseEntity<ApiResponse<List<MacroIndicatorResponse>>> getIndicators() {
        return ResponseEntity.ok(ApiResponse.success(macroService.getLatestIndicators()));
    }

    @GetMapping("/indicators/{indicatorCode}")
    @Operation(summary = "Get indicator history")
    public ResponseEntity<ApiResponse<List<MacroIndicatorResponse>>> getIndicatorHistory(
            @PathVariable @NotBlank String indicatorCode) {
        return ResponseEntity.ok(ApiResponse.success(
                macroService.getIndicatorHistory(indicatorCode)));
    }

    @GetMapping("/indicators/category/{category}")
    @Operation(summary = "Get indicators by category",
            description = "Returns latest indicators for the given category "
                    + "(POLICY, ECONOMY, FINANCIAL_MARKET, LIQUIDITY, COMMODITY, FOREX)")
    public ResponseEntity<ApiResponse<List<MacroIndicatorResponse>>> getIndicatorsByCategory(
            @PathVariable @NotBlank String category) {
        return ResponseEntity.ok(ApiResponse.success(
                macroService.getIndicatorsByCategory(category)));
    }

    @GetMapping("/risk-summary")
    @Operation(summary = "Get market risk indicator summary",
            description = "Returns integrated risk assessment based on VIX, Treasury yields, "
                    + "yield spread, DXY, and credit spread")
    public ResponseEntity<ApiResponse<MarketRiskResponse>> getRiskSummary() {
        return ResponseEntity.ok(ApiResponse.success(marketRiskService.getRiskSummary()));
    }
}
