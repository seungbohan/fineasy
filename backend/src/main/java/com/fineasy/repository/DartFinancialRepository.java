package com.fineasy.repository;

import com.fineasy.entity.DartFinancialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DartFinancialRepository extends JpaRepository<DartFinancialEntity, Long> {

    Optional<DartFinancialEntity> findByStockCodeAndBsnsYear(String stockCode, String bsnsYear);

    Optional<DartFinancialEntity> findTopByStockCodeOrderByBsnsYearDesc(String stockCode);

    @Query("SELECT d FROM DartFinancialEntity d WHERE d.stockCode = :stockCode " +
            "AND d.bsnsYear >= :fromYear AND d.bsnsYear <= :toYear " +
            "ORDER BY d.bsnsYear DESC")
    List<DartFinancialEntity> findByStockCodeAndBsnsYearRange(
            @Param("stockCode") String stockCode,
            @Param("fromYear") String fromYear,
            @Param("toYear") String toYear);
}
