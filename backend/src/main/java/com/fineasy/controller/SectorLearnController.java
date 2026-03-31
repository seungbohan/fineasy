package com.fineasy.controller;

import com.fineasy.dto.ApiResponse;
import com.fineasy.dto.response.SectorDetailResponse;
import com.fineasy.dto.response.SectorSummaryResponse;
import com.fineasy.service.SectorContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/learn/sectors")
@Tag(name = "Sector Learning", description = "Sector-based investment learning content")
@Validated
public class SectorLearnController {

    private final SectorContentService sectorContentService;

    public SectorLearnController(SectorContentService sectorContentService) {
        this.sectorContentService = sectorContentService;
    }

    @GetMapping
    @Operation(summary = "Get all sector summaries",
            description = "Returns a list of all sector learning content summaries")
    public ResponseEntity<ApiResponse<SectorSummaryResponse>> getAllSectors() {
        return ResponseEntity.ok(ApiResponse.success(sectorContentService.getAllSectors()));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get sector detail",
            description = "Returns detailed sector learning content including industry structure, value chain, trends, and representative companies")
    public ResponseEntity<ApiResponse<SectorDetailResponse>> getSectorDetail(
            @Parameter(description = "Sector slug (e.g., semiconductor, defense, secondary-battery)")
            @PathVariable @NotBlank String slug) {
        return ResponseEntity.ok(ApiResponse.success(sectorContentService.getSectorBySlug(slug)));
    }
}
