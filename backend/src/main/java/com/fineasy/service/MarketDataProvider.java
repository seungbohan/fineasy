package com.fineasy.service;

import com.fineasy.dto.response.StockRankingResponse;
import com.fineasy.entity.MarketIndex;

import java.time.Instant;
import java.util.List;

public interface MarketDataProvider {

    List<MarketIndex> getMarketIndices();

    String getMarketSummary();

    default StockRankingResponse getStockRanking(String type, int size) {
        return new StockRankingResponse(type, List.of(), Instant.now());
    }

    default StockRankingResponse getOverseasStockRanking(String type, int size) {
        return new StockRankingResponse(type, List.of(), Instant.now());
    }
}
