package com.fineasy.dto.response;

import java.time.LocalDateTime;

public record FeedbackResponse(
        Long id,
        String type,
        String title,
        String status,
        LocalDateTime createdAt
) {
}
