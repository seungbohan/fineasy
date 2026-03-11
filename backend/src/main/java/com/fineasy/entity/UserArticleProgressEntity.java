package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_article_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_progress_user_article",
                columnNames = {"user_id", "article_id"}))
public class UserArticleProgressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private LearnArticleEntity article;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    protected UserArticleProgressEntity() {
    }

    public UserArticleProgressEntity(Long id, UserEntity user, LearnArticleEntity article,
                                     LocalDateTime completedAt) {
        this.id = id;
        this.user = user;
        this.article = article;
        this.completedAt = completedAt;
    }

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public LearnArticleEntity getArticle() { return article; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
