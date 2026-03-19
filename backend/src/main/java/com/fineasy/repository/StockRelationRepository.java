package com.fineasy.repository;

import com.fineasy.entity.RelationType;
import com.fineasy.entity.StockRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockRelationRepository extends JpaRepository<StockRelationEntity, Long> {

    Optional<StockRelationEntity> findBySourceStockIdAndTargetStockIdAndRelationType(
            Long sourceStockId, Long targetStockId, RelationType relationType);

    @Query("SELECT r FROM StockRelationEntity r JOIN FETCH r.targetStock " +
            "WHERE r.sourceStock.id = :stockId AND r.validUntil >= :now " +
            "ORDER BY r.weight DESC")
    List<StockRelationEntity> findValidRelationsBySourceStock(
            @Param("stockId") Long stockId, @Param("now") LocalDate now);

    @Query("SELECT r FROM StockRelationEntity r JOIN FETCH r.targetStock " +
            "WHERE r.sourceStock.stockCode = :stockCode AND r.validUntil >= :now " +
            "AND r.weight >= :minWeight " +
            "ORDER BY r.weight DESC")
    List<StockRelationEntity> findValidRelationsByStockCode(
            @Param("stockCode") String stockCode,
            @Param("now") LocalDate now,
            @Param("minWeight") Double minWeight);

    @Modifying
    @Query("DELETE FROM StockRelationEntity r WHERE r.validUntil < :now")
    int deleteExpiredRelations(@Param("now") LocalDate now);
}
