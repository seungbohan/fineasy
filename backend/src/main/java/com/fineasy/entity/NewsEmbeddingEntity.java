package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_embeddings", indexes = {
        @Index(name = "idx_news_embeddings_news_id", columnList = "news_article_id", unique = true)
})
public class NewsEmbeddingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_article_id", nullable = false, unique = true)
    private NewsArticleEntity newsArticle;

    @Column(name = "embedding", columnDefinition = "vector(1536)", nullable = false)
    private String embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NewsEmbeddingEntity() {}

    public NewsEmbeddingEntity(NewsArticleEntity newsArticle, String embedding) {
        this.newsArticle = newsArticle;
        this.embedding = embedding;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public NewsArticleEntity getNewsArticle() { return newsArticle; }
    public String getEmbedding() { return embedding; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
