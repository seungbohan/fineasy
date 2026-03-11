package com.fineasy.repository;

import com.fineasy.entity.UserArticleProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserArticleProgressRepository extends JpaRepository<UserArticleProgressEntity, Long> {

    Optional<UserArticleProgressEntity> findByUserIdAndArticleId(Long userId, Long articleId);

    List<UserArticleProgressEntity> findByUserId(Long userId);

    boolean existsByUserIdAndArticleId(Long userId, Long articleId);
}
