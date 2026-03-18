package com.fineasy.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks", indexes = {
        @Index(name = "idx_stocks_code", columnList = "stock_code", unique = true),
        @Index(name = "idx_stocks_name", columnList = "stock_name")
})
public class StockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, unique = true, length = 20)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Market market;

    @Column(length = 100)
    private String sector;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "market_cap_usd", precision = 24, scale = 2)
    private BigDecimal marketCapUsd;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected StockEntity() {
    }

    public StockEntity(Long id, String stockCode, String stockName,
                       Market market, String sector,
                       boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.market = market;
        this.sector = sector;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateName(String stockName) { this.stockName = stockName; }
    public void updateMarketCapUsd(BigDecimal marketCapUsd) { this.marketCapUsd = marketCapUsd; }

    public Long getId() { return id; }
    public String getStockCode() { return stockCode; }
    public String getStockName() { return stockName; }
    public Market getMarket() { return market; }
    public String getSector() { return sector; }
    public boolean isActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BigDecimal getMarketCapUsd() { return marketCapUsd; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
