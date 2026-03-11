package com.fineasy.repository;

import com.fineasy.entity.WatchlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<WatchlistEntity, Long> {

    List<WatchlistEntity> findByUserId(Long userId);

    Optional<WatchlistEntity> findByUserIdAndStockId(Long userId, Long stockId);

    boolean existsByUserIdAndStockId(Long userId, Long stockId);

    int countByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM WatchlistEntity w WHERE w.user.id = :userId AND w.stock.id = :stockId")
    void deleteByUserIdAndStockId(@Param("userId") Long userId, @Param("stockId") Long stockId);
}
