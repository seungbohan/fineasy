package com.fineasy.controller;

import com.fineasy.dto.response.WatchlistBriefingResponse;
import com.fineasy.dto.response.WatchlistResponse;
import com.fineasy.service.WatchlistService;
import com.fineasy.security.AuthenticatedUser;
import com.fineasy.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watchlist")
@Tag(name = "Watchlist", description = "User watchlist management (authentication required)")
@Validated
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    @Operation(summary = "Get user watchlist")
    public ResponseEntity<ApiResponse<List<WatchlistResponse>>> getWatchlist(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.success(
                watchlistService.getWatchlist(user.id())));
    }

    @PostMapping("/{stockCode}")
    @Operation(summary = "Add stock to watchlist")
    public ResponseEntity<ApiResponse<String>> addToWatchlist(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @NotBlank String stockCode) {
        watchlistService.addToWatchlist(user.id(), stockCode);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Added to watchlist"));
    }

    @DeleteMapping("/{stockCode}")
    @Operation(summary = "Remove stock from watchlist")
    public ResponseEntity<ApiResponse<String>> removeFromWatchlist(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @NotBlank String stockCode) {
        watchlistService.removeFromWatchlist(user.id(), stockCode);
        return ResponseEntity.ok(ApiResponse.success("Removed from watchlist"));
    }

    @GetMapping("/briefing")
    @Operation(summary = "Get AI-generated watchlist briefing (cached daily)")
    public ResponseEntity<ApiResponse<WatchlistBriefingResponse>> getWatchlistBriefing(
            @AuthenticationPrincipal AuthenticatedUser user) {
        WatchlistBriefingResponse briefing = watchlistService.getWatchlistBriefing(user.id());
        if (briefing == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ApiResponse.success(briefing));
    }
}
