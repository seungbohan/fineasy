package com.fineasy.repository;

import com.fineasy.entity.StockAnalysisReportEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockAnalysisReportRepository extends JpaRepository<StockAnalysisReportEntity, Long> {

    Optional<StockAnalysisReportEntity> findByStockIdAndReportDate(Long stockId, LocalDate reportDate);

    @Query("SELECT r FROM StockAnalysisReportEntity r WHERE r.stock.id = :stockId " +
            "ORDER BY r.reportDate DESC")
    List<StockAnalysisReportEntity> findByStockIdOrderByDateDesc(
            @Param("stockId") Long stockId, Pageable pageable);
}
