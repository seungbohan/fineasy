package com.fineasy.repository;

import com.fineasy.entity.StockPriceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StockPriceRepository extends JpaRepository<StockPriceEntity, Long> {

    @Query("SELECT sp FROM StockPriceEntity sp WHERE sp.stock.id = :stockId " +
            "ORDER BY sp.tradeDate DESC")
    List<StockPriceEntity> findLatestByStockId(@Param("stockId") Long stockId, Pageable pageable);

    @Query("SELECT sp FROM StockPriceEntity sp WHERE sp.stock.id = :stockId " +
            "AND sp.tradeDate BETWEEN :fromDate AND :toDate ORDER BY sp.tradeDate ASC")
    List<StockPriceEntity> findByStockIdAndDateRange(
            @Param("stockId") Long stockId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    default Optional<StockPriceEntity> findLatestByStockId(Long stockId) {
        List<StockPriceEntity> result = findLatestByStockId(stockId,
                org.springframework.data.domain.PageRequest.of(0, 1));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Query("SELECT sp.stock.id FROM StockPriceEntity sp WHERE sp.tradeDate = :date")
    Set<Long> findStockIdsWithPriceOnDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(sp) FROM StockPriceEntity sp WHERE sp.stock.id = :stockId")
    long countByStockId(@Param("stockId") Long stockId);

    @Query("SELECT sp FROM StockPriceEntity sp WHERE sp.stock.id IN :stockIds " +
            "AND sp.tradeDate >= :since ORDER BY sp.stock.id, sp.tradeDate DESC")
    List<StockPriceEntity> findRecentByStockIds(
            @Param("stockIds") Collection<Long> stockIds,
            @Param("since") LocalDate since);
}
