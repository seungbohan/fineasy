package com.fineasy.controller;

import com.fineasy.dto.ApiResponse;
import com.fineasy.dto.request.KeywordAlertRequest;
import com.fineasy.dto.response.KeywordAlertResponse;
import com.fineasy.dto.response.NewsArticleResponse;
import com.fineasy.security.AuthenticatedUser;
import com.fineasy.service.KeywordAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts/keywords")
@Tag(name = "Keyword Alerts", description = "User keyword alert management (authentication required)")
@Validated
public class KeywordAlertController {

    private final KeywordAlertService keywordAlertService;

    public KeywordAlertController(KeywordAlertService keywordAlertService) {
        this.keywordAlertService = keywordAlertService;
    }

    @GetMapping
    @Operation(summary = "Get user keyword alerts")
    public ResponseEntity<ApiResponse<List<KeywordAlertResponse>>> getKeywords(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.success(
                keywordAlertService.getKeywords(user.id())));
    }

    @PostMapping
    @Operation(summary = "Add keyword alert (max 10)")
    public ResponseEntity<ApiResponse<KeywordAlertResponse>> addKeyword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody KeywordAlertRequest request) {
        KeywordAlertResponse response = keywordAlertService.addKeyword(
                user.id(), request.keyword());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete keyword alert")
    public ResponseEntity<ApiResponse<String>> deleteKeyword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive long id) {
        keywordAlertService.deleteKeyword(user.id(), id);
        return ResponseEntity.ok(ApiResponse.success("Keyword alert deleted"));
    }
}
