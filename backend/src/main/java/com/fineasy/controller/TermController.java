package com.fineasy.controller;

import com.fineasy.dto.response.FinancialTermResponse;
import com.fineasy.dto.response.TermCategoryResponse;
import com.fineasy.service.TermService;
import com.fineasy.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/terms")
@Tag(name = "Financial Terms", description = "Financial term dictionary")
@Validated
public class TermController {

    private final TermService termService;

    public TermController(TermService termService) {
        this.termService = termService;
    }

    @GetMapping
    @Operation(summary = "Get all financial terms")
    public ResponseEntity<ApiResponse<List<FinancialTermResponse>>> getTerms() {
        return ResponseEntity.ok(ApiResponse.success(termService.getAllTerms()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search financial terms")
    public ResponseEntity<ApiResponse<List<FinancialTermResponse>>> searchTerms(
            @RequestParam("q") @NotBlank String query) {
        return ResponseEntity.ok(ApiResponse.success(termService.searchTerms(query)));
    }

    @GetMapping("/{termId}")
    @Operation(summary = "Get term details with related terms")
    public ResponseEntity<ApiResponse<FinancialTermResponse>> getTerm(
            @PathVariable @Positive long termId) {
        return ResponseEntity.ok(ApiResponse.success(termService.getTermById(termId)));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all term categories")
    public ResponseEntity<ApiResponse<List<TermCategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(termService.getCategories()));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get terms by category")
    public ResponseEntity<ApiResponse<List<FinancialTermResponse>>> getTermsByCategory(
            @PathVariable @Positive long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(
                termService.getTermsByCategory(categoryId)));
    }
}
