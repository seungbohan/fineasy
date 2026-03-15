package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "keyword_alerts", indexes = {
        @Index(name = "idx_keyword_alert_user", columnList = "user_id"),
        @Index(name = "idx_keyword_alert_user_active", columnList = "user_id, is_active")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_keyword_alert_user_keyword",
        columnNames = {"user_id", "keyword"}
))
public class KeywordAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected KeywordAlertEntity() {
    }

    public KeywordAlertEntity(Long id, Long userId, String keyword,
                               boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.keyword = keyword;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getKeyword() { return keyword; }
    public boolean isActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void deactivate() {
        this.isActive = false;
    }
}
