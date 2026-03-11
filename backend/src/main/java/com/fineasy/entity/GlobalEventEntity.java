package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "global_events", indexes = {
        @Index(name = "idx_global_event_type_published",
                columnList = "event_type, published_at DESC"),
        @Index(name = "idx_global_event_risk_published",
                columnList = "risk_level, published_at DESC")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_global_event_source_url",
        columnNames = "source_url"
))
public class GlobalEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected GlobalEventEntity() {
    }

    public GlobalEventEntity(Long id, EventType eventType, String title, String summary,
                             String sourceUrl, String sourceName, RiskLevel riskLevel,
                             LocalDateTime publishedAt, LocalDateTime createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.title = title;
        this.summary = summary;
        this.sourceUrl = sourceUrl;
        this.sourceName = sourceName;
        this.riskLevel = riskLevel;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public EventType getEventType() { return eventType; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getSourceUrl() { return sourceUrl; }
    public String getSourceName() { return sourceName; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
