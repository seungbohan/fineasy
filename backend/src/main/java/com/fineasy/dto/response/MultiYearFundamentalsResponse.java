package com.fineasy.dto.response;

import java.util.List;

public record MultiYearFundamentalsResponse(
        String stockCode,
        String stockName,
        List<YearlyData> yearlyData
) {

    public record YearlyData(
            String bsnsYear,
            Long revenue,
            Long operatingProfit,
            Long netIncome,
            Double operatingMargin,
            Double roe,
            Double debtRatio,
            Double revenueGrowthRate
    ) {
    }
}
