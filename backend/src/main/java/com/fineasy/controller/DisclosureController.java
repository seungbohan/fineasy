package com.fineasy.controller;

import com.fineasy.dto.ApiResponse;
import com.fineasy.dto.response.DomesticDisclosureResponse;
import com.fineasy.dto.response.OverseasDisclosureResponse;
import com.fineasy.service.DisclosureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/disclosure")
@Tag(name = "Disclosure", description = "Corporate disclosure filings (DART / SEC EDGAR)")
@Validated
public class DisclosureController {

    private final DisclosureService disclosureService;

    public DisclosureController(DisclosureService disclosureService) {
        this.disclosureService = disclosureService;
    }

    @GetMapping("/domestic/{stockCode}")
    @Operation(summary = "Get DART disclosure list for a domestic stock")
    public ResponseEntity<ApiResponse<DomesticDisclosureResponse>> getDomesticDisclosures(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(
                disclosureService.getDomesticDisclosures(stockCode)));
    }

    @GetMapping("/overseas/{stockCode}")
    @Operation(summary = "Get SEC EDGAR filings for an overseas stock")
    public ResponseEntity<ApiResponse<OverseasDisclosureResponse>> getOverseasDisclosures(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(ApiResponse.success(
                disclosureService.getOverseasDisclosures(stockCode)));
    }
}
