package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "learn_articles")
public class LearnArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ArticleCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Difficulty difficulty;

    @Column(name = "estimated_read_minutes")
    private Integer estimatedReadMinutes;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected LearnArticleEntity() {
    }

    public LearnArticleEntity(Long id, String title, String content, ArticleCategory category,
                              Difficulty difficulty, Integer estimatedReadMinutes,
                              Integer displayOrder, boolean isPublished) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.difficulty = difficulty;
        this.estimatedReadMinutes = estimatedReadMinutes;
        this.displayOrder = displayOrder;
        this.isPublished = isPublished;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public ArticleCategory getCategory() { return category; }
    public Difficulty getDifficulty() { return difficulty; }
    public Integer getEstimatedReadMinutes() { return estimatedReadMinutes; }
    public Integer getDisplayOrder() { return displayOrder; }
    public boolean isPublished() { return isPublished; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
