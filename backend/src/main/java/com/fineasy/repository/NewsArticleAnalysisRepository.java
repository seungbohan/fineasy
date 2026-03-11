package com.fineasy.repository;

import com.fineasy.entity.NewsArticleAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsArticleAnalysisRepository extends JpaRepository<NewsArticleAnalysisEntity, Long> {

    Optional<NewsArticleAnalysisEntity> findByNewsArticleId(Long newsArticleId);
}
