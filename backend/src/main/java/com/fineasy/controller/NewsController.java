package com.fineasy.controller;

import com.fineasy.dto.response.*;
import com.fineasy.security.AuthenticatedUser;
import com.fineasy.service.KeywordAlertService;
import com.fineasy.service.NewsService;
import com.fineasy.entity.Sentiment;
import com.fineasy.dto.ApiResponse;
import com.fineasy.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/news")
@Tag(name = "News", description = "Financial news")
@Validated
public class NewsController {

    private final NewsService newsService;
    private final KeywordAlertService keywordAlertService;

    public NewsController(NewsService newsService,
                           KeywordAlertService keywordAlertService) {
        this.newsService = newsService;
        this.keywordAlertService = keywordAlertService;
    }

    @GetMapping
    @Operation(summary = "Get financial news with pagination and filters")
    public ResponseEntity<ApiResponse<PageResponse<NewsArticleResponse>>> getNews(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(required = false) Sentiment sentiment,
            @RequestParam(required = false) String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(
                newsService.getNews(page, size, sentiment, stockCode)));
    }

    @GetMapping("/{newsId}")
    @Operation(summary = "Get news article details")
    public ResponseEntity<ApiResponse<NewsArticleResponse>> getNewsById(
            @PathVariable @Positive long newsId) {
        return ResponseEntity.ok(ApiResponse.success(newsService.getNewsById(newsId)));
    }

    @GetMapping("/{newsId}/analysis")
    @Operation(summary = "Get AI analysis for a news article (on-demand generation with caching)")
    public ResponseEntity<ApiResponse<NewsAnalysisResponse>> getNewsAnalysis(
            @PathVariable @Positive long newsId) {
        return ResponseEntity.ok(ApiResponse.success(newsService.getNewsAnalysis(newsId)));
    }

    @GetMapping("/watchlist")
    @Operation(summary = "Get news for watchlist stocks by stock codes")
    public ResponseEntity<ApiResponse<List<WatchlistNewsResponse>>> getWatchlistNews(
            @RequestParam(required = false) List<String> stockCodes,
            @RequestParam(defaultValue = "8") @Min(1) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                newsService.getWatchlistNews(stockCodes, size)));
    }

    @GetMapping("/latest-count")
    @Operation(summary = "Get count of new articles since a given timestamp")
    public ResponseEntity<ApiResponse<NewsCountResponse>> getLatestNewsCount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(ApiResponse.success(
                newsService.getLatestNewsCount(since)));
    }

    @GetMapping("/stock-summary/{stockCode}")
    @Operation(summary = "Get AI-generated one-line summary of recent news for a stock")
    public ResponseEntity<ApiResponse<StockNewsSummaryResponse>> getStockNewsSummary(
            @PathVariable @NotBlank String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(
                newsService.getStockNewsSummary(stockCode)));
    }

    @GetMapping("/sentiment-trend/{stockCode}")
    @Operation(summary = "Get daily sentiment trend for a stock")
    public ResponseEntity<ApiResponse<SentimentTrendResponse>> getSentimentTrend(
            @PathVariable @NotBlank String stockCode,
            @RequestParam(defaultValue = "30") @Min(1) int days) {
        return ResponseEntity.ok(ApiResponse.success(
                newsService.getSentimentTrend(stockCode, days)));
    }

    @GetMapping("/keyword-matches")
    @Operation(summary = "Get news matching user keyword alerts")
    public ResponseEntity<ApiResponse<List<NewsArticleResponse>>> getKeywordMatchedNews(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.success(
                keywordAlertService.getKeywordMatchedNews(user.id())));
    }

    @GetMapping("/macro")
    @Operation(summary = "Get macroeconomic news")
    public ResponseEntity<ApiResponse<PageResponse<NewsArticleResponse>>> getMacroNews(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                newsService.getMacroNews(page, size)));
    }
}
