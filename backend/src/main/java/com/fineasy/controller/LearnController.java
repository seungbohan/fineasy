package com.fineasy.controller;

import com.fineasy.dto.response.LearnArticleResponse;
import com.fineasy.service.LearnService;
import com.fineasy.security.AuthenticatedUser;
import com.fineasy.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/learn")
@Tag(name = "Learning Center", description = "Educational content for investors")
@Validated
public class LearnController {

    private final LearnService learnService;

    public LearnController(LearnService learnService) {
        this.learnService = learnService;
    }

    @GetMapping("/articles")
    @Operation(summary = "Get all published learning articles")
    public ResponseEntity<ApiResponse<List<LearnArticleResponse>>> getArticles(
            @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = user != null ? user.id() : null;
        return ResponseEntity.ok(ApiResponse.success(learnService.getArticles(userId)));
    }

    @GetMapping("/articles/{articleId}")
    @Operation(summary = "Get article details")
    public ResponseEntity<ApiResponse<LearnArticleResponse>> getArticle(
            @PathVariable @Positive long articleId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = user != null ? user.id() : null;
        return ResponseEntity.ok(ApiResponse.success(
                learnService.getArticleById(articleId, userId)));
    }

    @PostMapping("/articles/{articleId}/complete")
    @Operation(summary = "Mark article as completed (authentication required)")
    public ResponseEntity<ApiResponse<String>> completeArticle(
            @PathVariable @Positive long articleId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        learnService.markAsComplete(user.id(), articleId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Article marked as completed"));
    }
}
