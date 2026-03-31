package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sector_contents", indexes = {
        @Index(name = "idx_sector_contents_slug", columnList = "slug", unique = true)
})
public class SectorContentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(name = "name_ko", nullable = false, length = 50)
    private String nameKo;

    @Column(name = "name_en", nullable = false, length = 50)
    private String nameEn;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_name", length = 50)
    private String iconName;

    @Column(name = "industry_structure", columnDefinition = "TEXT")
    private String industryStructure;

    @Column(name = "value_chain", columnDefinition = "TEXT")
    private String valueChain;

    @Column(name = "industry_trend", columnDefinition = "TEXT")
    private String industryTrend;

    @OneToMany(mappedBy = "sectorContent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SectorRepresentativeCompanyEntity> companies = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected SectorContentEntity() {
    }

    public SectorContentEntity(Long id, String slug, String nameKo, String nameEn,
                                String description, String iconName,
                                String industryStructure, String valueChain,
                                String industryTrend) {
        this.id = id;
        this.slug = slug;
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.description = description;
        this.iconName = iconName;
        this.industryStructure = industryStructure;
        this.valueChain = valueChain;
        this.industryTrend = industryTrend;
    }

    public void addCompany(SectorRepresentativeCompanyEntity company) {
        companies.add(company);
        company.setSectorContent(this);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getNameKo() { return nameKo; }
    public String getNameEn() { return nameEn; }
    public String getDescription() { return description; }
    public String getIconName() { return iconName; }
    public String getIndustryStructure() { return industryStructure; }
    public String getValueChain() { return valueChain; }
    public String getIndustryTrend() { return industryTrend; }
    public List<SectorRepresentativeCompanyEntity> getCompanies() { return companies; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
