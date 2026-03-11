package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "financial_terms",
        uniqueConstraints = @UniqueConstraint(name = "uk_financial_term_name", columnNames = "name"),
        indexes = {
                @Index(name = "idx_financial_terms_category", columnList = "category_id")
        })
public class FinancialTermEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TermCategoryEntity category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Difficulty difficulty;

    @Column(name = "simple_description", nullable = false, length = 500)
    private String simpleDescription;

    @Column(name = "detailed_description", columnDefinition = "TEXT")
    private String detailedDescription;

    @Column(name = "example_sentence", length = 500)
    private String exampleSentence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(
            name = "term_relations",
            joinColumns = @JoinColumn(name = "term_id"),
            inverseJoinColumns = @JoinColumn(name = "related_term_id")
    )
    private List<FinancialTermEntity> relatedTerms = new ArrayList<>();

    protected FinancialTermEntity() {
    }

    public FinancialTermEntity(Long id, String name, String nameEn, TermCategoryEntity category,
                               Difficulty difficulty, String simpleDescription,
                               String detailedDescription, String exampleSentence,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.nameEn = nameEn;
        this.category = category;
        this.difficulty = difficulty;
        this.simpleDescription = simpleDescription;
        this.detailedDescription = detailedDescription;
        this.exampleSentence = exampleSentence;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getNameEn() { return nameEn; }
    public TermCategoryEntity getCategory() { return category; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getSimpleDescription() { return simpleDescription; }
    public String getDetailedDescription() { return detailedDescription; }
    public String getExampleSentence() { return exampleSentence; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<FinancialTermEntity> getRelatedTerms() { return relatedTerms; }
}
