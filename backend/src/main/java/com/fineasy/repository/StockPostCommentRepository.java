package com.fineasy.repository;

import com.fineasy.entity.StockPostCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockPostCommentRepository extends JpaRepository<StockPostCommentEntity, Long> {

    @Query("SELECT c FROM StockPostCommentEntity c JOIN FETCH c.user " +
           "WHERE c.post.id = :postId AND c.id > :cursor " +
           "ORDER BY c.id ASC")
    List<StockPostCommentEntity> findByPostIdWithCursor(
            @Param("postId") Long postId,
            @Param("cursor") Long cursor,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM StockPostCommentEntity c JOIN FETCH c.user " +
           "WHERE c.post.id = :postId " +
           "ORDER BY c.id ASC")
    List<StockPostCommentEntity> findByPostIdOldest(
            @Param("postId") Long postId,
            org.springframework.data.domain.Pageable pageable);
}
