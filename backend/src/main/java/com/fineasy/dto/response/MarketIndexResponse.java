package com.fineasy.dto.response;

import java.time.Instant;
import java.util.List;

public record MarketIndexResponse(
        List<IndexData> indices,
        Instant updatedAt
) {

    public record IndexData(
            String code,
            String name,
            double currentValue,
            double changeAmount,
            double changeRate,
            List<Double> sparklineData
    ) {
    }
}
