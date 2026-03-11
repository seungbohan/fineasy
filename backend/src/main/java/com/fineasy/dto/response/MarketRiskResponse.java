package com.fineasy.dto.response;

import java.time.LocalDate;
import java.util.List;

public record MarketRiskResponse(
        String overallRiskLevel,
        int overallRiskScore,
        String riskComment,
        List<RiskIndicator> indicators,
        String yieldCurveStatus,
        LocalDate assessedAt
) {

    public record RiskIndicator(
            String code,
            String name,
            double value,
            String unit,
            Double changeAmount,
            Double changeRate,
            String riskLevel,
            String description,
            LocalDate recordDate
    ) {}
}
