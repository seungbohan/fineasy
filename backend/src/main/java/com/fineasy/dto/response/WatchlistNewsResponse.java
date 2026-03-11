package com.fineasy.dto.response;

import com.fineasy.entity.Sentiment;

import java.time.LocalDateTime;
import java.util.List;

public record WatchlistNewsResponse(
        long id,
        String title,
        String content,
        String originalUrl,
        String sourceName,
        LocalDateTime publishedAt,
        Sentiment sentiment,
        double sentimentScore,
        List<String> relatedStockTags
) {
}
