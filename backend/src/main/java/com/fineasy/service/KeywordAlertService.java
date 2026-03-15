package com.fineasy.service;

import com.fineasy.dto.response.KeywordAlertResponse;
import com.fineasy.dto.response.NewsArticleResponse;
import com.fineasy.entity.KeywordAlertEntity;
import com.fineasy.entity.NewsArticleEntity;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.repository.KeywordAlertRepository;
import com.fineasy.repository.NewsArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class KeywordAlertService {

    private static final Logger log = LoggerFactory.getLogger(KeywordAlertService.class);

    private static final int MAX_KEYWORDS_PER_USER = 10;

    private static final int KEYWORD_MATCH_NEWS_LIMIT = 20;

    private final KeywordAlertRepository keywordAlertRepository;
    private final NewsArticleRepository newsArticleRepository;

    public KeywordAlertService(KeywordAlertRepository keywordAlertRepository,
                                NewsArticleRepository newsArticleRepository) {
        this.keywordAlertRepository = keywordAlertRepository;
        this.newsArticleRepository = newsArticleRepository;
    }

    public List<KeywordAlertResponse> getKeywords(long userId) {
        return keywordAlertRepository.findActiveByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public KeywordAlertResponse addKeyword(long userId, String keyword) {
        String trimmedKeyword = keyword.trim();

        if (keywordAlertRepository.existsByUserIdAndKeywordAndIsActiveTrue(userId, trimmedKeyword)) {
            throw new IllegalArgumentException("Keyword already exists: " + trimmedKeyword);
        }

        long activeCount = keywordAlertRepository.countActiveByUserId(userId);
        if (activeCount >= MAX_KEYWORDS_PER_USER) {
            throw new IllegalArgumentException(
                    "Maximum keyword limit reached (" + MAX_KEYWORDS_PER_USER + ")");
        }

        KeywordAlertEntity entity = new KeywordAlertEntity(
                null, userId, trimmedKeyword, true, null);
        KeywordAlertEntity saved = keywordAlertRepository.save(entity);

        log.info("User {} added keyword alert: {}", userId, trimmedKeyword);
        return toResponse(saved);
    }

    @Transactional
    public void deleteKeyword(long userId, long keywordId) {
        KeywordAlertEntity entity = keywordAlertRepository.findByIdAndUserId(keywordId, userId)
                .orElseThrow(() -> new EntityNotFoundException("KeywordAlert", keywordId));
        keywordAlertRepository.delete(entity);
        log.info("User {} deleted keyword alert id={}", userId, keywordId);
    }

    public List<NewsArticleResponse> getKeywordMatchedNews(long userId) {
        List<KeywordAlertEntity> keywords = keywordAlertRepository.findActiveByUserId(userId);
        if (keywords.isEmpty()) {
            return List.of();
        }

        // Collect matched news from all keywords, deduplicate by article id
        Map<Long, NewsArticleEntity> uniqueArticles = new LinkedHashMap<>();
        for (KeywordAlertEntity keyword : keywords) {
            List<NewsArticleEntity> matched = newsArticleRepository
                    .findByTitleContainingKeyword(keyword.getKeyword(),
                            PageRequest.of(0, KEYWORD_MATCH_NEWS_LIMIT));
            for (NewsArticleEntity article : matched) {
                uniqueArticles.putIfAbsent(article.getId(), article);
            }
        }

        // Sort by publishedAt desc and limit
        return uniqueArticles.values().stream()
                .sorted(Comparator.comparing(
                        NewsArticleEntity::getPublishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(KEYWORD_MATCH_NEWS_LIMIT)
                .map(this::toNewsResponse)
                .toList();
    }

    private KeywordAlertResponse toResponse(KeywordAlertEntity entity) {
        return new KeywordAlertResponse(
                entity.getId(),
                entity.getKeyword(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }

    private NewsArticleResponse toNewsResponse(NewsArticleEntity article) {
        return new NewsArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getOriginalUrl(),
                article.getSourceName(),
                article.getPublishedAt(),
                article.getSentiment(),
                article.getSentimentScore() != null ? article.getSentimentScore() : 0.5
        );
    }
}
