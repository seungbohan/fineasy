package com.fineasy.controller;

import com.fineasy.dto.response.NewsAnalysisResponse;
import com.fineasy.dto.response.NewsArticleResponse;
import com.fineasy.dto.response.WatchlistNewsResponse;
import com.fineasy.service.NewsService;
import com.fineasy.entity.Sentiment;
import com.fineasy.dto.ApiResponse;
import com.fineasy.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/news")
@Tag(name = "News", description = "Financial news")
@Validated
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
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

    @GetMapping("/macro")
    @Operation(summary = "Get macroeconomic news")
    public ResponseEntity<ApiResponse<PageResponse<NewsArticleResponse>>> getMacroNews(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                newsService.getMacroNews(page, size)));
    }
}
