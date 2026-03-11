package com.fineasy.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CryptoMarketResponse(
        List<CoinData> coins,
        Instant updatedAt
) {

    public record CoinData(
            String symbol,
            String name,
            BigDecimal priceUsd,
            BigDecimal priceKrw,
            BigDecimal marketCapUsd,
            BigDecimal volume24hUsd,
            Double change24h,
            Double change7d,
            Instant recordedAt
    ) {}
}
