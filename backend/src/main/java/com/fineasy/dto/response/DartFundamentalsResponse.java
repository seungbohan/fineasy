package com.fineasy.dto.response;

import java.util.List;

public record DartFundamentalsResponse(
        String stockCode,
        String stockName,
        String bsnsYear,

        Long revenue,
        Long operatingProfit,
        Long netIncome,

        Long totalAssets,
        Long totalLiabilities,
        Long totalEquity,

        Long operatingCashFlow,

        Double operatingMargin,
        Double revenueGrowthRate,
        Double debtRatio,
        Double roe,

        List<String> evaluationTags,
        String summaryComment
) {
}
