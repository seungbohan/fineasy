package com.fineasy.dto.response;

import java.time.LocalDateTime;

public record StockPostResponse(
        Long id,
        String content,
        String authorNickname,
        int likeCount,
        int dislikeCount,
        int commentCount,
        String myReaction,
        boolean isDeleted,
        LocalDateTime createdAt
) {
}
