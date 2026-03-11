package com.fineasy.controller;

import com.fineasy.dto.response.MarketIndexResponse;
import com.fineasy.dto.response.MarketSummaryResponse;
import com.fineasy.dto.response.StockRankingResponse;
import com.fineasy.service.MarketService;
import com.fineasy.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/market")
@Tag(name = "Market", description = "Market indices and summary")
@Validated
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/indices")
    @Operation(summary = "Get major market indices")
    public ResponseEntity<ApiResponse<MarketIndexResponse>> getIndices() {
        return ResponseEntity.ok(ApiResponse.success(marketService.getMarketIndices()));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get AI-generated market summary for today")
    public ResponseEntity<ApiResponse<MarketSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(marketService.getMarketSummary()));
    }

    @GetMapping("/ranking")
    @Operation(summary = "Get stock ranking from the entire market (gainers, losers, volume)")
    public ResponseEntity<ApiResponse<StockRankingResponse>> getRanking(
            @RequestParam(defaultValue = "gainers")
            @Pattern(regexp = "gainers|losers|volume|trading_value", message = "type must be 'gainers', 'losers', 'volume', or 'trading_value'")
            String type,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "domestic")
            @Pattern(regexp = "domestic|overseas", message = "region must be 'domestic' or 'overseas'")
            String region) {
        return ResponseEntity.ok(ApiResponse.success(marketService.getStockRanking(type, size, region)));
    }
}
