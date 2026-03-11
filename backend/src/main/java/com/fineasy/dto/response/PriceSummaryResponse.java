package com.fineasy.dto.response;

import java.math.BigDecimal;

public record PriceSummaryResponse(
        BigDecimal lastClose,
        BigDecimal firstClose,
        BigDecimal highestHigh,
        BigDecimal lowestLow
) {
    public static final PriceSummaryResponse EMPTY =
            new PriceSummaryResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
}
