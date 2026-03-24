package com.fineasy.repository;

import com.fineasy.entity.StockPostReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockPostReactionRepository extends JpaRepository<StockPostReactionEntity, Long> {

    Optional<StockPostReactionEntity> findByPostIdAndUserId(Long postId, Long userId);

    List<StockPostReactionEntity> findByPostIdInAndUserId(List<Long> postIds, Long userId);
}
