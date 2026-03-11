package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bok_term_ai_explanations",
        indexes = {
                @Index(name = "idx_bok_term_ai_explanations_term_id", columnList = "bok_term_id", unique = true)
        })
public class BokTermAiExplanationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bok_term_id", nullable = false, unique = true)
    private BokTermEntity bokTerm;

    @Column(name = "simple_summary", columnDefinition = "TEXT", nullable = false)
    private String simpleSummary;

    @Column(name = "easy_explanation", columnDefinition = "TEXT", nullable = false)
    private String easyExplanation;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String example;

    @Column(name = "key_points", columnDefinition = "TEXT")
    private String keyPoints;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BokTermAiExplanationEntity() {
    }

    public BokTermAiExplanationEntity(BokTermEntity bokTerm, String simpleSummary,
                                       String easyExplanation, String example,
                                       String keyPoints) {
        this.bokTerm = bokTerm;
        this.simpleSummary = simpleSummary;
        this.easyExplanation = easyExplanation;
        this.example = example;
        this.keyPoints = keyPoints;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public BokTermEntity getBokTerm() { return bokTerm; }
    public String getSimpleSummary() { return simpleSummary; }
    public String getEasyExplanation() { return easyExplanation; }
    public String getExample() { return example; }
    public String getKeyPoints() { return keyPoints; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
