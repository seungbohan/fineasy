package com.fineasy.dto.response;

import com.fineasy.entity.ImpactDirection;
import com.fineasy.entity.ImpactType;
import com.fineasy.entity.Sentiment;

import java.time.LocalDateTime;
import java.util.List;

public record KeyNewsResponse(
        long id,
        String title,
        String originalUrl,
        String sourceName,
        LocalDateTime publishedAt,
        Sentiment sentiment,
        double sentimentScore,
        ImpactType impactType,
        ImpactDirection impactDirection,
        double relevanceScore,
        List<NewsArticleResponse.TaggedStockInfo> taggedStocks
) {}
