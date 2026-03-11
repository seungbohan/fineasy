package com.fineasy.repository;

import com.fineasy.entity.LearnArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearnArticleRepository extends JpaRepository<LearnArticleEntity, Long> {

    List<LearnArticleEntity> findByIsPublishedTrueOrderByDisplayOrderAsc();
}
