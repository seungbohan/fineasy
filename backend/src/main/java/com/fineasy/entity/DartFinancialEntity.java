package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dart_financial", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dart_financial_stock_year",
                columnNames = {"stock_code", "bsns_year"})
})
public class DartFinancialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "bsns_year", nullable = false, length = 4)
    private String bsnsYear;

    @Column(name = "revenue")
    private Long revenue;

    @Column(name = "operating_profit")
    private Long operatingProfit;

    @Column(name = "net_income")
    private Long netIncome;

    @Column(name = "total_assets")
    private Long totalAssets;

    @Column(name = "total_liabilities")
    private Long totalLiabilities;

    @Column(name = "total_equity")
    private Long totalEquity;

    @Column(name = "operating_cash_flow")
    private Long operatingCashFlow;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected DartFinancialEntity() {
    }

    public DartFinancialEntity(String stockCode, String bsnsYear,
                                Long revenue, Long operatingProfit, Long netIncome,
                                Long totalAssets, Long totalLiabilities, Long totalEquity,
                                Long operatingCashFlow) {
        this.stockCode = stockCode;
        this.bsnsYear = bsnsYear;
        this.revenue = revenue;
        this.operatingProfit = operatingProfit;
        this.netIncome = netIncome;
        this.totalAssets = totalAssets;
        this.totalLiabilities = totalLiabilities;
        this.totalEquity = totalEquity;
        this.operatingCashFlow = operatingCashFlow;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getStockCode() { return stockCode; }
    public String getBsnsYear() { return bsnsYear; }
    public Long getRevenue() { return revenue; }
    public Long getOperatingProfit() { return operatingProfit; }
    public Long getNetIncome() { return netIncome; }
    public Long getTotalAssets() { return totalAssets; }
    public Long getTotalLiabilities() { return totalLiabilities; }
    public Long getTotalEquity() { return totalEquity; }
    public Long getOperatingCashFlow() { return operatingCashFlow; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setOperatingCashFlow(Long operatingCashFlow) {
        this.operatingCashFlow = operatingCashFlow;
    }
}
