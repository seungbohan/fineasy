package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "news_articles", indexes = {
        @Index(name = "idx_news_published_at", columnList = "published_at DESC"),
        @Index(name = "idx_news_sentiment", columnList = "sentiment"),
        @Index(name = "idx_news_sentiment_published", columnList = "sentiment, published_at DESC"),
        @Index(name = "idx_news_title", columnList = "title")
},
        uniqueConstraints = @UniqueConstraint(
                name = "uk_news_original_url",
                columnNames = "original_url"
        ))
public class NewsArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "original_url", nullable = false, unique = true, length = 1000)
    private String originalUrl;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sentiment sentiment;

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany
    @JoinTable(
            name = "news_stock_tags",
            joinColumns = @JoinColumn(name = "news_article_id"),
            inverseJoinColumns = @JoinColumn(name = "stock_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_news_stock_tag",
                    columnNames = {"news_article_id", "stock_id"})
    )
    private List<StockEntity> taggedStocks = new ArrayList<>();

    protected NewsArticleEntity() {
    }

    public NewsArticleEntity(Long id, String title, String content, String originalUrl,
                             String sourceName, LocalDateTime publishedAt, Sentiment sentiment,
                             Double sentimentScore, LocalDateTime createdAt,
                             List<StockEntity> taggedStocks) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.originalUrl = originalUrl;
        this.sourceName = sourceName;
        this.publishedAt = publishedAt;
        this.sentiment = sentiment;
        this.sentimentScore = sentimentScore;
        this.createdAt = createdAt;
        this.taggedStocks = taggedStocks != null ? taggedStocks : new ArrayList<>();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getOriginalUrl() { return originalUrl; }
    public String getSourceName() { return sourceName; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public Sentiment getSentiment() { return sentiment; }
    public Double getSentimentScore() { return sentimentScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<StockEntity> getTaggedStocks() { return taggedStocks; }

    public void updateSentiment(Sentiment sentiment, Double sentimentScore) {
        this.sentiment = sentiment;
        this.sentimentScore = sentimentScore;
    }

    public void updateTitle(String title) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
    }
}
