package com.fineasy.controller;

import com.fineasy.dto.response.*;
import com.fineasy.service.NewsService;
import com.fineasy.service.StockService;
import com.fineasy.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
@Tag(name = "Stocks", description = "Stock information and data")
@Validated
public class StockController {

    private final StockService stockService;
    private final NewsService newsService;

    public StockController(StockService stockService, NewsService newsService) {
        this.stockService = stockService;
        this.newsService = newsService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search stocks by name or code")
    public ResponseEntity<ApiResponse<List<StockResponse>>> search(
            @RequestParam("q") @NotBlank String query) {
        return ResponseEntity.ok(ApiResponse.success(stockService.searchStocks(query)));
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular stocks, optionally filtered by region")
    public ResponseEntity<ApiResponse<List<StockResponse>>> popular(
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) int size) {
        if (region != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    stockService.getStocksByRegion(region, page, Math.min(size, 50))));
        }
        return ResponseEntity.ok(ApiResponse.success(stockService.getPopularStocks()));
    }

    @GetMapping("/{stockCode}")
    @Operation(summary = "Get stock basic information")
    public ResponseEntity<ApiResponse<StockResponse>> getStock(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getStockInfo(stockCode)));
    }

    @GetMapping("/{stockCode}/price")
    @Operation(summary = "Get current stock price")
    public ResponseEntity<ApiResponse<StockPriceResponse>> getPrice(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getStockPrice(stockCode)));
    }

    @GetMapping("/{stockCode}/chart")
    @Operation(summary = "Get stock chart data")
    public ResponseEntity<ApiResponse<StockChartResponse>> getChart(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "1M") String period,
            @RequestParam(defaultValue = "LINE") String type) {
        return ResponseEntity.ok(ApiResponse.success(
                stockService.getChartData(stockCode, period, type)));
    }

    @GetMapping("/{stockCode}/financials")
    @Operation(summary = "Get stock financial data")
    public ResponseEntity<ApiResponse<StockFinancialsResponse>> getFinancials(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getFinancials(stockCode)));
    }

    @GetMapping("/{stockCode}/fundamentals")
    @Operation(summary = "Get stock DART fundamentals (income statement, balance sheet, derived ratios)")
    public ResponseEntity<ApiResponse<DartFundamentalsResponse>> getFundamentals(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getDartFundamentals(stockCode)));
    }

    @GetMapping("/{stockCode}/fundamentals/history")
    @Operation(summary = "Get multi-year (3~5 years) financial fundamentals for trend analysis")
    public ResponseEntity<ApiResponse<MultiYearFundamentalsResponse>> getFundamentalsHistory(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getMultiYearFundamentals(stockCode)));
    }

    @GetMapping("/{stockCode}/sector-comparison")
    @Operation(summary = "Get sector (peer) comparison data for relative valuation")
    public ResponseEntity<ApiResponse<SectorComparisonResponse>> getSectorComparison(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getSectorComparison(stockCode)));
    }

    @GetMapping("/{stockCode}/news")
    @Operation(summary = "Get news related to a specific stock")
    public ResponseEntity<ApiResponse<List<NewsArticleResponse>>> getStockNews(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(
                newsService.getNewsByStockCode(stockCode, 20)));
    }
}
