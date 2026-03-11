package com.fineasy.dto.response;

import java.time.LocalDateTime;

public record WatchlistBriefingResponse(
        String briefing,
        LocalDateTime generatedAt,
        String disclaimer
) {
    public static final String AI_DISCLAIMER =
            "본 브리핑은 AI가 자동 생성한 요약이며, 투자 권유가 아닙니다. 투자 판단의 근거로 사용하지 마세요.";
}
