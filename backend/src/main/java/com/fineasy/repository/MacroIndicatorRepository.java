package com.fineasy.repository;

import com.fineasy.entity.MacroIndicatorEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MacroIndicatorRepository extends JpaRepository<MacroIndicatorEntity, Long> {

    @Query("SELECT m FROM MacroIndicatorEntity m WHERE m.recordDate = " +
            "(SELECT MAX(m2.recordDate) FROM MacroIndicatorEntity m2 " +
            "WHERE m2.indicatorCode = m.indicatorCode)")
    List<MacroIndicatorEntity> findLatestIndicators();

    @Query("SELECT m FROM MacroIndicatorEntity m WHERE m.indicatorCode = :code " +
            "ORDER BY m.recordDate DESC")
    List<MacroIndicatorEntity> findByCodeOrderByDateDesc(
            @Param("code") String code, Pageable pageable);

    default Optional<MacroIndicatorEntity> findLatestByCode(String code) {
        List<MacroIndicatorEntity> result = findByCodeOrderByDateDesc(code,
                org.springframework.data.domain.PageRequest.of(0, 1));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Query("SELECT m FROM MacroIndicatorEntity m WHERE m.recordDate = " +
            "(SELECT MAX(m2.recordDate) FROM MacroIndicatorEntity m2 " +
            "WHERE m2.indicatorCode = m.indicatorCode " +
            "AND m2.recordDate < (SELECT MAX(m3.recordDate) FROM MacroIndicatorEntity m3 " +
            "WHERE m3.indicatorCode = m.indicatorCode))")
    List<MacroIndicatorEntity> findPreviousIndicators();

    boolean existsByIndicatorCodeAndRecordDate(String code, LocalDate date);

    Optional<MacroIndicatorEntity> findByIndicatorCodeAndRecordDate(String indicatorCode, LocalDate recordDate);

    boolean existsByIndicatorCodeAndRecordDateAfter(String code, LocalDate date);

    long deleteByIndicatorCode(String indicatorCode);

    @Query("SELECT m FROM MacroIndicatorEntity m WHERE m.indicatorCode IN :codes " +
            "AND m.recordDate = (SELECT MAX(m2.recordDate) FROM MacroIndicatorEntity m2 " +
            "WHERE m2.indicatorCode = m.indicatorCode)")
    List<MacroIndicatorEntity> findLatestByIndicatorCodes(@Param("codes") List<String> codes);

    @Query("SELECT m FROM MacroIndicatorEntity m WHERE m.indicatorCode IN :codes " +
            "AND m.recordDate = (SELECT MAX(m2.recordDate) FROM MacroIndicatorEntity m2 " +
            "WHERE m2.indicatorCode = m.indicatorCode " +
            "AND m2.recordDate < (SELECT MAX(m3.recordDate) FROM MacroIndicatorEntity m3 " +
            "WHERE m3.indicatorCode = m.indicatorCode))")
    List<MacroIndicatorEntity> findPreviousByIndicatorCodes(@Param("codes") List<String> codes);
}
