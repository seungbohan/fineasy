package com.fineasy.dto.response;

import com.fineasy.entity.Sentiment;

import java.time.LocalDateTime;
import java.util.List;

public record NewsArticleResponse(
        long id,
        String title,
        String content,
        String originalUrl,
        String sourceName,
        LocalDateTime publishedAt,
        Sentiment sentiment,
        double sentimentScore,
        List<TaggedStockInfo> taggedStocks
) {
    public record TaggedStockInfo(
            String stockCode,
            String stockName
    ) {}
}
