package com.fineasy.dto.response;

import java.math.BigDecimal;

public record StockFinancialsResponse(
        String stockCode,
        String stockName,
        BigDecimal marketCap,
        long sharesOutstanding,
        Double per,
        Double pbr,
        Double eps,
        Double dividendYield,
        BigDecimal high52Week,
        BigDecimal low52Week
) {
}
