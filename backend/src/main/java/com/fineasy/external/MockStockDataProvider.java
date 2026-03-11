package com.fineasy.external;

import com.fineasy.dto.response.StockFinancialsResponse;
import com.fineasy.service.StockDataProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "data-provider.type", havingValue = "mock", matchIfMissing = true)
public class MockStockDataProvider implements StockDataProvider {

    private static final Map<String, BigDecimal> MOCK_PRICES = Map.of(
            "005930", new BigDecimal("72000"),
            "000660", new BigDecimal("132000"),
            "035720", new BigDecimal("58000"),
            "035420", new BigDecimal("310000"),
            "051910", new BigDecimal("450000"),
            "006400", new BigDecimal("65000"),
            "003670", new BigDecimal("85000"),
            "005380", new BigDecimal("235000"),
            "AAPL", new BigDecimal("182.50"),
            "MSFT", new BigDecimal("415.00")
    );

    @Override
    public BigDecimal getCurrentPrice(String stockCode) {
        return MOCK_PRICES.getOrDefault(stockCode, new BigDecimal("50000"));
    }

    @Override
    public StockFinancialsResponse getFinancials(String stockCode) {
        return new StockFinancialsResponse(
                stockCode,
                getStockName(stockCode),
                new BigDecimal("430000000000000"),
                5969782550L,
                12.3,
                1.2,
                5837.0,
                2.1,
                new BigDecimal("78000"),
                new BigDecimal("59000")
        );
    }

    private String getStockName(String stockCode) {
        return switch (stockCode) {
            case "005930" -> "삼성전자";
            case "000660" -> "SK하이닉스";
            case "035720" -> "카카오";
            case "035420" -> "NAVER";
            default -> "Unknown";
        };
    }
}
