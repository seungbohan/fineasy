package com.fineasy.dto.response;

import java.util.List;

public record OverseasDisclosureResponse(
        String stockCode,
        String companyName,
        int totalCount,
        List<FilingItem> filings
) {
    public record FilingItem(
            String accessionNumber,
            String filingType,
            String filingDate,
            String description,
            String secUrl
    ) {}
}
