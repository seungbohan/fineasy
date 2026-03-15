package com.fineasy.repository;

import com.fineasy.entity.KeywordAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KeywordAlertRepository extends JpaRepository<KeywordAlertEntity, Long> {

    @Query("SELECT k FROM KeywordAlertEntity k WHERE k.userId = :userId AND k.isActive = true " +
            "ORDER BY k.createdAt DESC")
    List<KeywordAlertEntity> findActiveByUserId(@Param("userId") long userId);

    @Query("SELECT COUNT(k) FROM KeywordAlertEntity k WHERE k.userId = :userId AND k.isActive = true")
    long countActiveByUserId(@Param("userId") long userId);

    Optional<KeywordAlertEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndKeywordAndIsActiveTrue(Long userId, String keyword);
}
