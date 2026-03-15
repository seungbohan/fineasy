package com.fineasy.dto.response;

import java.util.List;

public record DomesticDisclosureResponse(
        String stockCode,
        String corpName,
        int totalCount,
        List<DisclosureItem> disclosures
) {
    public record DisclosureItem(
            String receiptNumber,
            String reportName,
            String filerName,
            String receiptDate,
            String disclosureType,
            String dartUrl
    ) {}
}
