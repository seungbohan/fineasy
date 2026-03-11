package com.fineasy.dto.response;

import com.fineasy.entity.Market;

public record StockResponse(
        long id,
        String stockCode,
        String stockName,
        Market market,
        String sector
) {
}
