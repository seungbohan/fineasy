package com.fineasy.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AnalysisReportResponse(
        String stockCode,
        LocalDateTime generatedAt,
        String summary,
        String description,
        List<String> keyPoints,
        String investmentOpinion,
        String disclaimer,
        Map<String, Object> technicalSignals
) {
}
