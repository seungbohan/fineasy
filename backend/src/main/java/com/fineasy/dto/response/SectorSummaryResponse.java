package com.fineasy.dto.response;

import java.util.List;

public record SectorSummaryResponse(
        List<SectorSummary> sectors
) {

    public record SectorSummary(
            Long id,
            String slug,
            String nameKo,
            String nameEn,
            String description,
            String iconName
    ) {}
}
