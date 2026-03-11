package com.fineasy.repository;

import com.fineasy.entity.StockPredictionEntity;
import com.fineasy.entity.TargetPeriod;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockPredictionRepository extends JpaRepository<StockPredictionEntity, Long> {

    @Query("SELECT p FROM StockPredictionEntity p WHERE p.stock.id = :stockId " +
            "ORDER BY p.predictionDate DESC")
    List<StockPredictionEntity> findByStockIdOrderByDateDesc(
            @Param("stockId") Long stockId, Pageable pageable);

    Optional<StockPredictionEntity> findByStockIdAndPredictionDateAndTargetPeriod(
            Long stockId, LocalDate predictionDate, TargetPeriod targetPeriod);
}
