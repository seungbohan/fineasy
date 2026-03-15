package com.fineasy.dto.response;

import java.time.LocalDateTime;

public record StockNewsSummaryResponse(
        String stockCode,
        String summary,
        int newsCount,
        LocalDateTime generatedAt,
        String disclaimer
) {

    public static final String AI_DISCLAIMER =
            "본 요약은 AI가 자동 생성한 것으로 투자 권유가 아닙니다. " +
            "투자 판단의 책임은 투자자 본인에게 있습니다.";

    public static StockNewsSummaryResponse empty(String stockCode) {
        return new StockNewsSummaryResponse(stockCode, null, 0, LocalDateTime.now(), AI_DISCLAIMER);
    }
}
