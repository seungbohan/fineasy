package com.fineasy.dto.response;

import java.util.List;

public record StockPostCommentListResponse(
        List<StockPostCommentResponse> comments,
        Long nextCursor,
        boolean hasNext
) {
}
