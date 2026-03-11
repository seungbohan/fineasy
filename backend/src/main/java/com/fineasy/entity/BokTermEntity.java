package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bok_terms",
        indexes = {
                @Index(name = "idx_bok_terms_term", columnList = "term")
        })
public class BokTermEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String term;

    @Column(name = "english_term", length = 200)
    private String englishTerm;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(length = 50)
    private String category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BokTermEntity() {
    }

    public BokTermEntity(Long id, String term, String englishTerm,
                          String definition, String category, LocalDateTime createdAt) {
        this.id = id;
        this.term = term;
        this.englishTerm = englishTerm;
        this.definition = definition;
        this.category = category;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTerm() { return term; }
    public String getEnglishTerm() { return englishTerm; }
    public String getDefinition() { return definition; }
    public String getCategory() { return category; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
