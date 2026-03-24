package com.fineasy.dto.response;

import java.util.List;

public record StockPostListResponse(
        List<StockPostResponse> posts,
        Long nextCursor,
        boolean hasNext
) {
}
