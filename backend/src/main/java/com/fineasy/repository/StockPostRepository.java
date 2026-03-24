package com.fineasy.repository;

import com.fineasy.entity.StockPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockPostRepository extends JpaRepository<StockPostEntity, Long> {

    @Query("SELECT p FROM StockPostEntity p JOIN FETCH p.user " +
           "WHERE p.stockCode = :stockCode AND p.id < :cursor " +
           "ORDER BY p.id DESC")
    List<StockPostEntity> findByStockCodeWithCursor(
            @Param("stockCode") String stockCode,
            @Param("cursor") Long cursor,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT p FROM StockPostEntity p JOIN FETCH p.user " +
           "WHERE p.stockCode = :stockCode " +
           "ORDER BY p.id DESC")
    List<StockPostEntity> findByStockCodeLatest(
            @Param("stockCode") String stockCode,
            org.springframework.data.domain.Pageable pageable);

    long countByStockCode(String stockCode);

    @Modifying
    @Query("UPDATE StockPostEntity p SET p.likeCount = p.likeCount + :delta WHERE p.id = :postId")
    void updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE StockPostEntity p SET p.dislikeCount = p.dislikeCount + :delta WHERE p.id = :postId")
    void updateDislikeCount(@Param("postId") Long postId, @Param("delta") int delta);
}
