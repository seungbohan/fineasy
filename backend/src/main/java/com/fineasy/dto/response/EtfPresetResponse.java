package com.fineasy.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record EtfPresetResponse(
        List<EtfPresetData> presets
) {

    public record EtfPresetData(
            Long id,
            String ticker,
            String nameEn,
            String nameKo,
            String market,
            String category,
            BigDecimal annualReturnRate,
            String description
    ) {}
}
