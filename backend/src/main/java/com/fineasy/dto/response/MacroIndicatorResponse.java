package com.fineasy.dto.response;

import java.time.LocalDate;

public record MacroIndicatorResponse(
        long id,
        String indicatorCode,
        String indicatorName,
        double value,
        String unit,
        LocalDate recordDate,
        String source,
        Double changeAmount,
        Double changeRate
) {

    public MacroIndicatorResponse(long id, String indicatorCode, String indicatorName,
                                   double value, String unit, LocalDate recordDate, String source) {
        this(id, indicatorCode, indicatorName, value, unit, recordDate, source, null, null);
    }
}
