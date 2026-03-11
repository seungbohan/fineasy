package com.fineasy.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record StockChartResponse(
        String stockCode,
        String stockName,
        String period,
        List<CandleData> candles,
        Map<String, List<Double>> indicators
) {

    public record CandleData(
            String date,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            long volume
    ) {
    }
}
