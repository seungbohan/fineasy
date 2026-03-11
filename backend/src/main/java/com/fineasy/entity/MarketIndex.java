package com.fineasy.entity;

import java.time.Instant;
import java.util.List;

public record MarketIndex(
        String code,
        String name,
        double currentValue,
        double changeAmount,
        double changeRate,
        List<Double> sparklineData,
        Instant updatedAt
) {
    public MarketIndex {
        if (sparklineData == null) {
            sparklineData = List.of();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
