package com.fineasy.service;

import com.fineasy.dto.response.LearnArticleResponse;
import com.fineasy.entity.LearnArticleEntity;
import com.fineasy.entity.UserArticleProgressEntity;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.repository.LearnArticleRepository;
import com.fineasy.repository.UserArticleProgressRepository;
import com.fineasy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LearnService {

    private final LearnArticleRepository articleRepository;

    private final UserArticleProgressRepository progressRepository;

    private final UserRepository userRepository;

    public LearnService(LearnArticleRepository articleRepository,
                        UserArticleProgressRepository progressRepository,
                        UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
    }

    public List<LearnArticleResponse> getArticles(Long userId) {
        List<LearnArticleEntity> articles = articleRepository
                .findByIsPublishedTrueOrderByDisplayOrderAsc();

        Set<Long> completedArticleIds = Set.of();
        if (userId != null) {
            completedArticleIds = progressRepository.findByUserId(userId).stream()
                    .map(p -> p.getArticle().getId())
                    .collect(Collectors.toSet());
        }

        Set<Long> finalCompletedIds = completedArticleIds;
        return articles.stream()
                .map(a -> toResponse(a, finalCompletedIds.contains(a.getId())))
                .toList();
    }

    public LearnArticleResponse getArticleById(long articleId, Long userId) {
        LearnArticleEntity article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("LearnArticle", articleId));

        boolean completed = userId != null
                && progressRepository.existsByUserIdAndArticleId(userId, articleId);

        return toResponse(article, completed);
    }

    @Transactional
    public void markAsComplete(long userId, long articleId) {
        LearnArticleEntity article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("LearnArticle", articleId));

        if (!progressRepository.existsByUserIdAndArticleId(userId, articleId)) {
            UserArticleProgressEntity progress = new UserArticleProgressEntity(
                    null,
                    userRepository.getReferenceById(userId),
                    article,
                    LocalDateTime.now()
            );
            progressRepository.save(progress);
        }
    }

    private LearnArticleResponse toResponse(LearnArticleEntity article, boolean completed) {
        return new LearnArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getCategory(),
                article.getDifficulty(),
                article.getEstimatedReadMinutes(),
                completed
        );
    }
}
