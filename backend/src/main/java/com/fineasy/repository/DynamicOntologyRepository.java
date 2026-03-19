package com.fineasy.repository;

import com.fineasy.entity.DynamicOntologyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DynamicOntologyRepository extends JpaRepository<DynamicOntologyEntity, Long> {

    @Query("SELECT d FROM DynamicOntologyEntity d " +
            "WHERE d.validUntil >= :now " +
            "AND (d.sector = :sector OR d.sector IS NULL) " +
            "ORDER BY d.createdAt DESC")
    List<DynamicOntologyEntity> findValidBySector(
            @Param("sector") String sector, @Param("now") LocalDate now);

    @Query("SELECT d FROM DynamicOntologyEntity d " +
            "WHERE d.validUntil >= :now " +
            "AND d.relatedStocks LIKE %:stockCode% " +
            "ORDER BY d.createdAt DESC")
    List<DynamicOntologyEntity> findValidByRelatedStock(
            @Param("stockCode") String stockCode, @Param("now") LocalDate now);

    @Query("SELECT d FROM DynamicOntologyEntity d " +
            "WHERE d.validUntil >= :now " +
            "AND (d.sector = :sector OR d.sector IS NULL OR d.relatedStocks LIKE %:stockCode%) " +
            "ORDER BY d.createdAt DESC")
    List<DynamicOntologyEntity> findValidBySectorOrStock(
            @Param("sector") String sector,
            @Param("stockCode") String stockCode,
            @Param("now") LocalDate now);

    @Modifying
    @Query("DELETE FROM DynamicOntologyEntity d WHERE d.validUntil < :now")
    int deleteExpired(@Param("now") LocalDate now);

    @Modifying
    @Query("DELETE FROM DynamicOntologyEntity d WHERE d.sector = :sector")
    void deleteBySector(@Param("sector") String sector);
}
