package com.fineasy.dto.response;

import com.fineasy.entity.Sentiment;

import java.time.LocalDateTime;

public record NewsArticleResponse(
        long id,
        String title,
        String content,
        String originalUrl,
        String sourceName,
        LocalDateTime publishedAt,
        Sentiment sentiment,
        double sentimentScore
) {
}
