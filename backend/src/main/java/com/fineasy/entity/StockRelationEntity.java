package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_relations", indexes = {
        @Index(name = "idx_sr_source", columnList = "source_stock_id"),
        @Index(name = "idx_sr_target", columnList = "target_stock_id"),
        @Index(name = "idx_sr_source_type", columnList = "source_stock_id, relation_type"),
        @Index(name = "idx_sr_valid", columnList = "valid_until")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_relation",
        columnNames = {"source_stock_id", "target_stock_id", "relation_type"}))
public class StockRelationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_stock_id", nullable = false)
    private StockEntity sourceStock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_stock_id", nullable = false)
    private StockEntity targetStock;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 20)
    private RelationType relationType;

    @Column(nullable = false)
    private Double weight;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected StockRelationEntity() {
    }

    public StockRelationEntity(StockEntity sourceStock, StockEntity targetStock,
                                RelationType relationType, Double weight,
                                LocalDate validUntil) {
        this.sourceStock = sourceStock;
        this.targetStock = targetStock;
        this.relationType = relationType;
        this.weight = weight;
        this.validUntil = validUntil;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public StockEntity getSourceStock() { return sourceStock; }
    public StockEntity getTargetStock() { return targetStock; }
    public RelationType getRelationType() { return relationType; }
    public Double getWeight() { return weight; }
    public LocalDate getValidUntil() { return validUntil; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void updateWeight(Double weight) {
        this.weight = weight;
    }

    public void extendValidity(LocalDate validUntil) {
        this.validUntil = validUntil;
    }
}
