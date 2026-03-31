package com.fineasy.service;

import com.fineasy.dto.response.SectorDetailResponse;
import com.fineasy.dto.response.SectorSummaryResponse;
import com.fineasy.entity.SectorContentEntity;
import com.fineasy.entity.SectorRepresentativeCompanyEntity;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.repository.SectorContentRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SectorContentService {

    private final SectorContentRepository sectorContentRepository;

    public SectorContentService(SectorContentRepository sectorContentRepository) {
        this.sectorContentRepository = sectorContentRepository;
    }

    @Cacheable(value = "sector-contents-list", unless = "#result == null")
    public SectorSummaryResponse getAllSectors() {
        List<SectorContentEntity> entities = sectorContentRepository.findAllOrderByNameKo();

        List<SectorSummaryResponse.SectorSummary> sectors = entities.stream()
                .map(e -> new SectorSummaryResponse.SectorSummary(
                        e.getId(),
                        e.getSlug(),
                        e.getNameKo(),
                        e.getNameEn(),
                        e.getDescription(),
                        e.getIconName()
                ))
                .toList();

        return new SectorSummaryResponse(sectors);
    }

    @Cacheable(value = "sector-contents-detail", key = "#slug", unless = "#result == null")
    public SectorDetailResponse getSectorBySlug(String slug) {
        SectorContentEntity entity = sectorContentRepository.findBySlugWithCompanies(slug)
                .orElseThrow(() -> new EntityNotFoundException("SectorContent", slug));

        List<SectorDetailResponse.CompanyData> companies = entity.getCompanies().stream()
                .sorted(Comparator.comparingInt(c ->
                        c.getDisplayOrder() != null ? c.getDisplayOrder() : Integer.MAX_VALUE))
                .map(c -> new SectorDetailResponse.CompanyData(
                        c.getCompanyName(),
                        c.getStockCode(),
                        c.getMarket(),
                        c.getRoleDescription()
                ))
                .toList();

        return new SectorDetailResponse(
                entity.getSlug(),
                entity.getNameKo(),
                entity.getNameEn(),
                entity.getDescription(),
                entity.getIndustryStructure(),
                entity.getValueChain(),
                entity.getIndustryTrend(),
                companies
        );
    }
}
