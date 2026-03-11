package com.fineasy.dto.response;

import com.fineasy.entity.Difficulty;

import java.util.List;

public record FinancialTermResponse(
        long id,
        String name,
        String nameEn,
        String category,
        Difficulty difficulty,
        String simpleDescription,
        String detailedDescription,
        String exampleSentence,
        List<RelatedTerm> relatedTerms
) {

    public record RelatedTerm(long id, String name) {
    }
}
