package com.fineasy.dto.response;

import java.time.Instant;

public record MarketSummaryResponse(
        String summary,
        String sentiment,
        String sentimentLabel,
        String overview,
        String macro,
        String news,
        String tip,
        Instant generatedAt
) {
}
