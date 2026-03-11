package com.fineasy.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockPriceResponse(
        String stockCode,
        String stockName,
        BigDecimal currentPrice,
        BigDecimal changeAmount,
        double changeRate,
        long volume,
        LocalDate tradeDate
) {
}
