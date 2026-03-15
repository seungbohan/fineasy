package com.fineasy.external.kis;

import com.fineasy.entity.Market;

public final class KisExchangeCodeMapper {

    private KisExchangeCodeMapper() {

    }

    public static String toExchangeCode(Market market) {
        return switch (market) {
            case NASDAQ -> "NAS";
            case NYSE -> "NYS";
            case AMEX -> "AMS";
            default -> throw new IllegalArgumentException(
                    "No KIS exchange code mapping for market: " + market);
        };
    }

    public static boolean isOverseasStockCode(String stockCode) {
        if (stockCode == null || stockCode.isEmpty()) {
            return false;
        }

        return !stockCode.matches("\\d{6}");
    }

    public static boolean isOverseasMarket(Market market) {
        return market == Market.NASDAQ || market == Market.NYSE || market == Market.AMEX;
    }
}
