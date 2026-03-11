package com.fineasy.repository;

import com.fineasy.entity.EventType;
import com.fineasy.entity.GlobalEventEntity;
import com.fineasy.entity.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface GlobalEventRepository extends JpaRepository<GlobalEventEntity, Long> {

    Page<GlobalEventEntity> findByEventTypeOrderByPublishedAtDesc(
            EventType eventType, Pageable pageable);

    Page<GlobalEventEntity> findByRiskLevelInOrderByPublishedAtDesc(
            List<RiskLevel> levels, Pageable pageable);

    @Query("SELECT e FROM GlobalEventEntity e " +
            "WHERE e.riskLevel IN (com.fineasy.entity.RiskLevel.HIGH, com.fineasy.entity.RiskLevel.CRITICAL) " +
            "AND e.publishedAt >= :since " +
            "ORDER BY e.publishedAt DESC")
    List<GlobalEventEntity> findRecent24h(@Param("since") java.time.LocalDateTime since);

    boolean existsBySourceUrl(String sourceUrl);

    @Query("SELECT e.sourceUrl FROM GlobalEventEntity e WHERE e.sourceUrl IN :urls")
    Set<String> findExistingSourceUrls(@Param("urls") Collection<String> urls);

    @Query("SELECT e FROM GlobalEventEntity e ORDER BY e.publishedAt DESC")
    Page<GlobalEventEntity> findAllOrderByPublishedAtDesc(Pageable pageable);
}
