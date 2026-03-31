package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sector_representative_companies", indexes = {
        @Index(name = "idx_sector_rep_company_sector", columnList = "sector_id")
})
public class SectorRepresentativeCompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private SectorContentEntity sectorContent;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(length = 20)
    private String market;

    @Column(name = "role_description", columnDefinition = "TEXT")
    private String roleDescription;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SectorRepresentativeCompanyEntity() {
    }

    public SectorRepresentativeCompanyEntity(String companyName, String stockCode,
                                              String market, String roleDescription,
                                              Integer displayOrder) {
        this.companyName = companyName;
        this.stockCode = stockCode;
        this.market = market;
        this.roleDescription = roleDescription;
        this.displayOrder = displayOrder;
    }

    void setSectorContent(SectorContentEntity sectorContent) {
        this.sectorContent = sectorContent;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public SectorContentEntity getSectorContent() { return sectorContent; }
    public String getCompanyName() { return companyName; }
    public String getStockCode() { return stockCode; }
    public String getMarket() { return market; }
    public String getRoleDescription() { return roleDescription; }
    public Integer getDisplayOrder() { return displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
