package com.fineasy.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record BokTermExplanationResponse(
        long termId,
        String term,
        String simpleSummary,
        String easyExplanation,
        String example,
        List<String> keyPoints,
        LocalDateTime generatedAt
) {
}
