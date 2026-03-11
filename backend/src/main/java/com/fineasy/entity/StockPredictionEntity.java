package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_predictions", indexes = {
        @Index(name = "idx_prediction_stock_date",
                columnList = "stock_id, prediction_date DESC")
})
public class StockPredictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private StockEntity stock;

    @Column(name = "prediction_date", nullable = false)
    private LocalDate predictionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_period", length = 20)
    private TargetPeriod targetPeriod;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PredictionDirection direction;

    private Integer confidence;

    @Column(columnDefinition = "TEXT")
    private String reasons;

    @Column(length = 20)
    private String valuation;

    @Column(length = 500)
    private String disclaimer;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    protected StockPredictionEntity() {
    }

    public StockPredictionEntity(Long id, StockEntity stock, LocalDate predictionDate,
                                 TargetPeriod targetPeriod, PredictionDirection direction,
                                 Integer confidence, String reasons, String valuation,
                                 String disclaimer, LocalDateTime generatedAt) {
        this.id = id;
        this.stock = stock;
        this.predictionDate = predictionDate;
        this.targetPeriod = targetPeriod;
        this.direction = direction;
        this.confidence = confidence;
        this.reasons = reasons;
        this.valuation = valuation;
        this.disclaimer = disclaimer;
        this.generatedAt = generatedAt;
    }

    public Long getId() { return id; }
    public StockEntity getStock() { return stock; }
    public LocalDate getPredictionDate() { return predictionDate; }
    public TargetPeriod getTargetPeriod() { return targetPeriod; }
    public PredictionDirection getDirection() { return direction; }
    public Integer getConfidence() { return confidence; }
    public String getReasons() { return reasons; }
    public String getValuation() { return valuation; }
    public String getDisclaimer() { return disclaimer; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
}
