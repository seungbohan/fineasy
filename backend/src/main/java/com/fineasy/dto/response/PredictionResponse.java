package com.fineasy.dto.response;

import com.fineasy.entity.PredictionDirection;

import java.time.LocalDateTime;
import java.util.List;

public record PredictionResponse(
        String stockCode,
        String period,
        PredictionDirection direction,
        int confidence,
        List<String> reasons,
        String disclaimer,
        LocalDateTime generatedAt,
        String valuation
) {
}
