package com.fineasy.service;

import com.fineasy.dto.response.StockChartResponse;
import com.fineasy.dto.response.StockFinancialsResponse;
import com.fineasy.dto.response.StockPriceResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface StockDataProvider {

    BigDecimal getCurrentPrice(String stockCode);

    default StockPriceResponse getStockPriceDetail(String stockCode, String stockName) {
        BigDecimal price = getCurrentPrice(stockCode);
        return new StockPriceResponse(
                stockCode, stockName, price,
                BigDecimal.ZERO, 0.0, 0L, LocalDate.now()
        );
    }

    default List<StockChartResponse.CandleData> getDailyPrices(String stockCode, LocalDate from, LocalDate to) {
        return List.of();
    }

    default List<StockChartResponse.CandleData> getMinutePrices(String stockCode) {
        return List.of();
    }

    StockFinancialsResponse getFinancials(String stockCode);
}
