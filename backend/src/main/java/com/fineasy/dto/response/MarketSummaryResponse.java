package com.fineasy.dto.response;

import java.time.Instant;

public record MarketSummaryResponse(
        String summary,
        Instant generatedAt
) {
}
