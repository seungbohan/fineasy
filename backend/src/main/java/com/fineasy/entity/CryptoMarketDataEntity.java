package com.fineasy.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "crypto_market_data", indexes = {
        @Index(name = "idx_crypto_symbol_recorded",
                columnList = "symbol, recorded_at DESC")
})
public class CryptoMarketDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "price_usd", precision = 20, scale = 8)
    private BigDecimal priceUsd;

    @Column(name = "price_krw", precision = 20, scale = 2)
    private BigDecimal priceKrw;

    @Column(name = "market_cap_usd", precision = 24, scale = 2)
    private BigDecimal marketCapUsd;

    @Column(name = "volume_24h_usd", precision = 24, scale = 2)
    private BigDecimal volume24hUsd;

    @Column(name = "change_24h")
    private Double change24h;

    @Column(name = "change_7d")
    private Double change7d;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(length = 30)
    private String source;

    protected CryptoMarketDataEntity() {
    }

    public CryptoMarketDataEntity(Long id, String symbol, String name,
                                   BigDecimal priceUsd, BigDecimal priceKrw,
                                   BigDecimal marketCapUsd, BigDecimal volume24hUsd,
                                   Double change24h, Double change7d,
                                   Instant recordedAt, String source) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.priceUsd = priceUsd;
        this.priceKrw = priceKrw;
        this.marketCapUsd = marketCapUsd;
        this.volume24hUsd = volume24hUsd;
        this.change24h = change24h;
        this.change7d = change7d;
        this.recordedAt = recordedAt;
        this.source = source;
    }

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public BigDecimal getPriceUsd() { return priceUsd; }
    public BigDecimal getPriceKrw() { return priceKrw; }
    public BigDecimal getMarketCapUsd() { return marketCapUsd; }
    public BigDecimal getVolume24hUsd() { return volume24hUsd; }
    public Double getChange24h() { return change24h; }
    public Double getChange7d() { return change7d; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getSource() { return source; }
}
