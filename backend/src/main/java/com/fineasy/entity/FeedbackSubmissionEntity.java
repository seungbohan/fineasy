package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_submissions", indexes = {
        @Index(name = "idx_feedback_ip_created", columnList = "ip_address, created_at"),
        @Index(name = "idx_feedback_status", columnList = "status"),
        @Index(name = "idx_feedback_type", columnList = "type")
})
public class FeedbackSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status = FeedbackStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected FeedbackSubmissionEntity() {
    }

    public FeedbackSubmissionEntity(FeedbackType type, String title, String content,
                                    String contactEmail, String ipAddress) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.contactEmail = contactEmail;
        this.ipAddress = ipAddress;
        this.status = FeedbackStatus.PENDING;
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
    public FeedbackType getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getContactEmail() { return contactEmail; }
    public String getIpAddress() { return ipAddress; }
    public FeedbackStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
