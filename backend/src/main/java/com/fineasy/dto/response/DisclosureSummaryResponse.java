package com.fineasy.dto.response;

import java.util.List;

public record DisclosureSummaryResponse(
        String receiptNumber,
        String reportName,
        String filerName,
        String filingDate,
        String disclosureType,
        String dartUrl,
        DisclosureSummary summary
) {

    public static final String AI_DISCLAIMER =
            "본 요약은 AI가 자동 생성한 것으로 투자 권유가 아닙니다. " +
            "정확한 내용은 원문(DART)을 확인해 주세요.";

    public record DisclosureSummary(
            String overview,
            String keyPoints,
            List<String> highlights,
            String investorImplication,
            String disclaimer
    ) {
    }
}
