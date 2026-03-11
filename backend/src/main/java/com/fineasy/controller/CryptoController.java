package com.fineasy.controller;

import com.fineasy.dto.ApiResponse;
import com.fineasy.dto.response.CryptoMarketResponse;
import com.fineasy.service.CryptoMarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crypto")
@Tag(name = "Cryptocurrency", description = "Cryptocurrency market data from CoinGecko")
@Validated
public class CryptoController {

    private final CryptoMarketService cryptoMarketService;

    public CryptoController(CryptoMarketService cryptoMarketService) {
        this.cryptoMarketService = cryptoMarketService;
    }

    @GetMapping
    @Operation(summary = "Get all crypto prices",
            description = "Returns latest price data for all tracked cryptocurrencies (BTC, ETH, XRP, etc.)")
    public ResponseEntity<ApiResponse<CryptoMarketResponse>> getAllPrices() {
        return ResponseEntity.ok(ApiResponse.success(cryptoMarketService.getLatestPrices()));
    }

    @GetMapping("/{symbol}")
    @Operation(summary = "Get specific coin detail",
            description = "Returns latest price data for a specific cryptocurrency")
    public ResponseEntity<ApiResponse<CryptoMarketResponse>> getCoinDetail(
            @Parameter(description = "Coin symbol (e.g., BTC, ETH, XRP)")
            @PathVariable @NotBlank String symbol) {
        return ResponseEntity.ok(ApiResponse.success(cryptoMarketService.getCoinDetail(symbol)));
    }

    @GetMapping("/{symbol}/history")
    @Operation(summary = "Get coin price history",
            description = "Returns historical price data for a specific cryptocurrency")
    public ResponseEntity<ApiResponse<CryptoMarketResponse>> getCoinHistory(
            @Parameter(description = "Coin symbol (e.g., BTC, ETH, XRP)")
            @PathVariable @NotBlank String symbol,
            @Parameter(description = "Number of days to look back (default: 7, max: 30)")
            @RequestParam(defaultValue = "7") @Min(1) @Max(30) int days) {
        return ResponseEntity.ok(ApiResponse.success(
                cryptoMarketService.getCoinHistory(symbol, days)));
    }
}
