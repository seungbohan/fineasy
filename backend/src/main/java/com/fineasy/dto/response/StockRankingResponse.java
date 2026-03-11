package com.fineasy.dto.response;

import java.time.Instant;
import java.util.List;

public record StockRankingResponse(
        String type,
        List<RankedStock> stocks,
        Instant updatedAt
) {

    public record RankedStock(
            int rank,
            String stockCode,
            String stockName,
            double currentPrice,
            double changeAmount,
            double changeRate,
            long volume,
            double tradingValue
    ) {
    }
}
