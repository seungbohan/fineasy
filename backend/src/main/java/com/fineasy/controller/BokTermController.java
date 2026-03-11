package com.fineasy.controller;

import com.fineasy.dto.ApiResponse;
import com.fineasy.dto.PageResponse;
import com.fineasy.dto.response.BokTermExplanationResponse;
import com.fineasy.dto.response.BokTermResponse;
import com.fineasy.service.BokTermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bok-terms")
@Tag(name = "BOK Terms", description = "Bank of Korea financial term dictionary (700+ terms)")
@Validated
public class BokTermController {

    private final BokTermService bokTermService;

    public BokTermController(BokTermService bokTermService) {
        this.bokTermService = bokTermService;
    }

    @GetMapping
    @Operation(summary = "Search BOK financial terms with pagination")
    public ResponseEntity<ApiResponse<PageResponse<BokTermResponse>>> searchTerms(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                bokTermService.searchTerms(keyword, page, size)));
    }

    @GetMapping("/{termId}")
    @Operation(summary = "Get BOK term details by ID")
    public ResponseEntity<ApiResponse<BokTermResponse>> getTermById(
            @PathVariable @Positive long termId) {
        return ResponseEntity.ok(ApiResponse.success(
                bokTermService.getTermById(termId)));
    }

    @GetMapping("/random")
    @Operation(summary = "Get a random BOK term (term of the day)")
    public ResponseEntity<ApiResponse<BokTermResponse>> getRandomTerm() {
        return ResponseEntity.ok(ApiResponse.success(
                bokTermService.getRandomTerm()));
    }

    @GetMapping("/{termId}/explanation")
    @Operation(summary = "Get AI-generated easy explanation for a BOK term")
    public ResponseEntity<ApiResponse<BokTermExplanationResponse>> getExplanation(
            @PathVariable @Positive long termId) {
        return ResponseEntity.ok(ApiResponse.success(
                bokTermService.getAiExplanation(termId)));
    }
}
