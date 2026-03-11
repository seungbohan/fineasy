package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_analysis_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_stock_date",
                columnNames = {"stock_id", "report_date"}),
        indexes = {
                @Index(name = "idx_report_stock_date",
                        columnList = "stock_id, report_date DESC")
        })
public class StockAnalysisReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private StockEntity stock;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "key_points", columnDefinition = "TEXT")
    private String keyPoints;

    @Column(name = "technical_signals", columnDefinition = "TEXT")
    private String technicalSignals;

    @Column(name = "investment_opinion", length = 10)
    private String investmentOpinion;

    @Column(length = 500)
    private String disclaimer;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    protected StockAnalysisReportEntity() {
    }

    public StockAnalysisReportEntity(Long id, StockEntity stock, LocalDate reportDate,
                                     String summary, String description, String keyPoints,
                                     String technicalSignals, String investmentOpinion,
                                     String disclaimer, LocalDateTime generatedAt) {
        this.id = id;
        this.stock = stock;
        this.reportDate = reportDate;
        this.summary = summary;
        this.description = description;
        this.keyPoints = keyPoints;
        this.technicalSignals = technicalSignals;
        this.investmentOpinion = investmentOpinion;
        this.disclaimer = disclaimer;
        this.generatedAt = generatedAt;
    }

    public Long getId() { return id; }
    public StockEntity getStock() { return stock; }
    public LocalDate getReportDate() { return reportDate; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getKeyPoints() { return keyPoints; }
    public String getTechnicalSignals() { return technicalSignals; }
    public String getInvestmentOpinion() { return investmentOpinion; }
    public String getDisclaimer() { return disclaimer; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
}
