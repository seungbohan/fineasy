package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_article_analyses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_analysis_news_article",
                columnNames = "news_article_id"),
        indexes = {
                @Index(name = "idx_analysis_created_at", columnList = "created_at DESC")
        })
public class NewsArticleAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_article_id", nullable = false)
    private NewsArticleEntity newsArticle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "market_impact", nullable = false, columnDefinition = "TEXT")
    private String marketImpact;

    @Column(name = "related_stocks", columnDefinition = "TEXT")
    private String relatedStocks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sentiment sentiment;

    @Column(name = "key_takeaway", nullable = false, length = 500)
    private String keyTakeaway;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NewsArticleAnalysisEntity() {
    }

    public NewsArticleAnalysisEntity(Long id, NewsArticleEntity newsArticle,
                                      String summary, String marketImpact,
                                      String relatedStocks, Sentiment sentiment,
                                      String keyTakeaway, LocalDateTime createdAt) {
        this.id = id;
        this.newsArticle = newsArticle;
        this.summary = summary;
        this.marketImpact = marketImpact;
        this.relatedStocks = relatedStocks;
        this.sentiment = sentiment;
        this.keyTakeaway = keyTakeaway;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public NewsArticleEntity getNewsArticle() { return newsArticle; }
    public String getSummary() { return summary; }
    public String getMarketImpact() { return marketImpact; }
    public String getRelatedStocks() { return relatedStocks; }
    public Sentiment getSentiment() { return sentiment; }
    public String getKeyTakeaway() { return keyTakeaway; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
