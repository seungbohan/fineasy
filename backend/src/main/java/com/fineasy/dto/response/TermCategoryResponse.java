package com.fineasy.dto.response;

public record TermCategoryResponse(
        long id,
        String name,
        int displayOrder
) {
}
