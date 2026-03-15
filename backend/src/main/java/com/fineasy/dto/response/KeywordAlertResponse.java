package com.fineasy.dto.response;

import java.time.LocalDateTime;

public record KeywordAlertResponse(
        long id,
        String keyword,
        boolean isActive,
        LocalDateTime createdAt
) {
}
