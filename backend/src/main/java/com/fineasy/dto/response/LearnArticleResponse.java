package com.fineasy.dto.response;

import com.fineasy.entity.ArticleCategory;
import com.fineasy.entity.Difficulty;

public record LearnArticleResponse(
        long id,
        String title,
        String content,
        ArticleCategory category,
        Difficulty difficulty,
        int estimatedReadMinutes,
        boolean completed
) {
}
