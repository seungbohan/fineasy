package com.fineasy.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WatchlistResponse(
        long id,
        String stockCode,
        String stockName,
        BigDecimal currentPrice,
        BigDecimal changeAmount,
        double changeRate,
        LocalDateTime addedAt
) {
}
