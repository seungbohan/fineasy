package com.fineasy.dto.response;

import com.fineasy.entity.Sentiment;

import java.time.LocalDateTime;
import java.util.List;

public record NewsAnalysisResponse(
        long newsId,
        String title,
        String source,
        LocalDateTime publishedAt,
        Analysis analysis,
        String disclaimer
) {

    public static final String AI_DISCLAIMER =
            "본 분석은 AI가 자동 생성한 것으로 투자 권유가 아닙니다. " +
            "투자 판단의 책임은 투자자 본인에게 있으며, " +
            "실제 투자 시 전문가 상담을 권장합니다.";

    public record Analysis(
            String summary,
            String marketImpact,
            List<String> relatedStocks,
            Sentiment sentiment,
            String keyTakeaway
    ) {
    }
}
