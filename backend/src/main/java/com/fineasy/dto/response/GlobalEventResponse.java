package com.fineasy.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record GlobalEventResponse(
        List<EventData> events,
        long totalCount,
        int page,
        int size
) {

    public record EventData(
            Long id,
            String eventType,
            String title,
            String summary,
            String sourceUrl,
            String sourceName,
            String riskLevel,
            LocalDateTime publishedAt
    ) {}
}
