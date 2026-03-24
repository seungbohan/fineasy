package com.fineasy.controller;

import com.fineasy.dto.ApiResponse;
import com.fineasy.dto.request.CreateCommentRequest;
import com.fineasy.dto.request.CreatePostRequest;
import com.fineasy.dto.request.ReactionRequest;
import com.fineasy.dto.response.*;
import com.fineasy.security.AuthenticatedUser;
import com.fineasy.service.StockPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Stock Discussion", description = "Stock community discussion board")
public class StockPostController {

    private final StockPostService stockPostService;

    public StockPostController(StockPostService stockPostService) {
        this.stockPostService = stockPostService;
    }

    // ── Posts ──

    @GetMapping("/api/v1/stocks/{stockCode}/posts")
    @Operation(summary = "Get posts for a stock (cursor-based pagination)")
    public ResponseEntity<ApiResponse<StockPostListResponse>> getPosts(
            @PathVariable String stockCode,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = user != null ? user.id() : null;
        return ResponseEntity.ok(ApiResponse.success(
                stockPostService.getPosts(stockCode, cursor, size, userId)));
    }

    @PostMapping("/api/v1/stocks/{stockCode}/posts")
    @Operation(summary = "Create a new post")
    public ResponseEntity<ApiResponse<StockPostResponse>> createPost(
            @PathVariable String stockCode,
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        StockPostResponse post = stockPostService.createPost(stockCode, request.content(), user.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(post));
    }

    @DeleteMapping("/api/v1/stocks/{stockCode}/posts/{postId}")
    @Operation(summary = "Delete a post (soft delete, owner only)")
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable String stockCode,
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        stockPostService.deletePost(postId, user.id());
        return ResponseEntity.ok(ApiResponse.success("Post deleted"));
    }

    @GetMapping("/api/v1/stocks/{stockCode}/posts/count")
    @Operation(summary = "Get post count for a stock")
    public ResponseEntity<ApiResponse<PostCountResponse>> getPostCount(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(stockPostService.getPostCount(stockCode)));
    }

    // ── Reactions ──

    @PostMapping("/api/v1/posts/{postId}/reactions")
    @Operation(summary = "Toggle reaction on a post (LIKE/DISLIKE)")
    public ResponseEntity<ApiResponse<ReactionResponse>> toggleReaction(
            @PathVariable Long postId,
            @Valid @RequestBody ReactionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ReactionResponse response = stockPostService.toggleReaction(
                postId, request.reactionType(), user.id());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Comments ──

    @GetMapping("/api/v1/posts/{postId}/comments")
    @Operation(summary = "Get comments for a post (cursor-based pagination)")
    public ResponseEntity<ApiResponse<StockPostCommentListResponse>> getComments(
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                stockPostService.getComments(postId, cursor, size)));
    }

    @PostMapping("/api/v1/posts/{postId}/comments")
    @Operation(summary = "Add a comment to a post")
    public ResponseEntity<ApiResponse<StockPostCommentResponse>> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        StockPostCommentResponse comment = stockPostService.createComment(
                postId, request.content(), user.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(comment));
    }

    @DeleteMapping("/api/v1/posts/{postId}/comments/{commentId}")
    @Operation(summary = "Delete a comment (soft delete, owner only)")
    public ResponseEntity<ApiResponse<String>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        stockPostService.deleteComment(postId, commentId, user.id());
        return ResponseEntity.ok(ApiResponse.success("Comment deleted"));
    }
}
