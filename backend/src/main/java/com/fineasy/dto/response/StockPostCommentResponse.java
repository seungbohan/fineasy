package com.fineasy.dto.response;

import java.time.LocalDateTime;

public record StockPostCommentResponse(
        Long id,
        String content,
        String authorNickname,
        boolean isDeleted,
        LocalDateTime createdAt
) {
}
