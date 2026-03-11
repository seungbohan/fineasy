package com.fineasy.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_prices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_prices_stock_date",
                columnNames = {"stock_id", "trade_date"}),
        indexes = {
                @Index(name = "idx_stock_prices_stock_date",
                        columnList = "stock_id, trade_date DESC")
        })
public class StockPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private StockEntity stock;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "open_price", precision = 15, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "high_price", precision = 15, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 15, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "close_price", precision = 15, scale = 2)
    private BigDecimal closePrice;

    private Long volume;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected StockPriceEntity() {
    }

    public StockPriceEntity(Long id, StockEntity stock, LocalDate tradeDate,
                            BigDecimal openPrice, BigDecimal highPrice,
                            BigDecimal lowPrice, BigDecimal closePrice,
                            Long volume, LocalDateTime createdAt) {
        this.id = id;
        this.stock = stock;
        this.tradeDate = tradeDate;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public StockEntity getStock() { return stock; }
    public LocalDate getTradeDate() { return tradeDate; }
    public BigDecimal getOpenPrice() { return openPrice; }
    public BigDecimal getHighPrice() { return highPrice; }
    public BigDecimal getLowPrice() { return lowPrice; }
    public BigDecimal getClosePrice() { return closePrice; }
    public Long getVolume() { return volume; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
