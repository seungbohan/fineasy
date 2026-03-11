package com.fineasy.dto.response;

public record BokTermResponse(
        long id,
        String term,
        String englishTerm,
        String definition,
        String category
) {
}
