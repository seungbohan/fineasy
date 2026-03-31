package com.fineasy.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "etf_presets", indexes = {
        @Index(name = "idx_etf_presets_ticker", columnList = "ticker", unique = true)
})
public class EtfPresetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String ticker;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    @Column(nullable = false, length = 20)
    private String market;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "annual_return_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualReturnRate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected EtfPresetEntity() {
    }

    public EtfPresetEntity(Long id, String ticker, String nameEn, String nameKo,
                           String market, String category, BigDecimal annualReturnRate,
                           String description) {
        this.id = id;
        this.ticker = ticker;
        this.nameEn = nameEn;
        this.nameKo = nameKo;
        this.market = market;
        this.category = category;
        this.annualReturnRate = annualReturnRate;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTicker() { return ticker; }
    public String getNameEn() { return nameEn; }
    public String getNameKo() { return nameKo; }
    public String getMarket() { return market; }
    public String getCategory() { return category; }
    public BigDecimal getAnnualReturnRate() { return annualReturnRate; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
