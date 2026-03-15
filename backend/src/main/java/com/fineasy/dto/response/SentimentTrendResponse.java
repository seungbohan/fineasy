package com.fineasy.dto.response;

import java.time.LocalDate;
import java.util.List;

public record SentimentTrendResponse(
        String stockCode,
        int days,
        List<DailySentiment> trend
) {

    public record DailySentiment(
            LocalDate date,
            double avgScore,
            int count,
            int positive,
            int negative,
            int neutral
    ) {
    }
}
