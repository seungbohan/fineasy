package com.fineasy.dto.response;

import java.util.List;

public record SectorDetailResponse(
        String slug,
        String nameKo,
        String nameEn,
        String description,
        String industryStructure,
        String valueChain,
        String industryTrend,
        List<CompanyData> companies
) {

    public record CompanyData(
            String companyName,
            String stockCode,
            String market,
            String roleDescription
    ) {}
}
