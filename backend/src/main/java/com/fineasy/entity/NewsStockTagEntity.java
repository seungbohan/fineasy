package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_stock_tags_v2", indexes = {
        @Index(name = "idx_nst2_article", columnList = "news_article_id"),
        @Index(name = "idx_nst2_stock", columnList = "stock_id"),
        @Index(name = "idx_nst2_stock_impact", columnList = "stock_id, impact_type"),
        @Index(name = "idx_nst2_stock_direction", columnList = "stock_id, impact_direction")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_nst2_article_stock",
        columnNames = {"news_article_id", "stock_id"}))
public class NewsStockTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_article_id", nullable = false)
    private NewsArticleEntity newsArticle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private StockEntity stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact_type", length = 20)
    private ImpactType impactType;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact_direction", length = 20)
    private ImpactDirection impactDirection;

    @Column(name = "relevance_score")
    private Double relevanceScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NewsStockTagEntity() {
    }

    public NewsStockTagEntity(NewsArticleEntity newsArticle, StockEntity stock,
                               ImpactType impactType, ImpactDirection impactDirection,
                               Double relevanceScore) {
        this.newsArticle = newsArticle;
        this.stock = stock;
        this.impactType = impactType;
        this.impactDirection = impactDirection;
        this.relevanceScore = relevanceScore;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public NewsArticleEntity getNewsArticle() { return newsArticle; }
    public StockEntity getStock() { return stock; }
    public ImpactType getImpactType() { return impactType; }
    public ImpactDirection getImpactDirection() { return impactDirection; }
    public Double getRelevanceScore() { return relevanceScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void updateImpact(ImpactType impactType, ImpactDirection impactDirection,
                              Double relevanceScore) {
        this.impactType = impactType;
        this.impactDirection = impactDirection;
        this.relevanceScore = relevanceScore;
    }
}
