package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_ontology", indexes = {
        @Index(name = "idx_do_type_sector", columnList = "entry_type, sector"),
        @Index(name = "idx_do_valid", columnList = "valid_until"),
        @Index(name = "idx_do_related_stocks", columnList = "related_stocks")
})
public class DynamicOntologyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_type", nullable = false, length = 30)
    private String entryType;  // HOT_ISSUE, STOCK_RELATION, EMERGING_THEME

    @Column(length = 50)
    private String sector;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "related_stocks", length = 200)
    private String relatedStocks;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected DynamicOntologyEntity() {
    }

    public DynamicOntologyEntity(String entryType, String sector, String subject,
                                  String description, String relatedStocks,
                                  LocalDate validFrom, LocalDate validUntil) {
        this.entryType = entryType;
        this.sector = sector;
        this.subject = subject;
        this.description = description;
        this.relatedStocks = relatedStocks;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEntryType() { return entryType; }
    public String getSector() { return sector; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getRelatedStocks() { return relatedStocks; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidUntil() { return validUntil; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
