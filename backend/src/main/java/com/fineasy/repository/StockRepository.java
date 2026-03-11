package com.fineasy.repository;

import com.fineasy.entity.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<StockEntity, Long> {

    Optional<StockEntity> findByStockCode(String stockCode);

    boolean existsByStockCode(String stockCode);

    @Query("SELECT s FROM StockEntity s WHERE s.isActive = true AND " +
            "(LOWER(s.stockName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.stockCode) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<StockEntity> searchByNameOrCode(@Param("query") String query);

    @Query("SELECT s FROM StockEntity s WHERE s.isActive = true ORDER BY s.id ASC")
    List<StockEntity> findPopularStocks(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT s FROM StockEntity s WHERE s.isActive = true AND s.market = :market ORDER BY s.id ASC")
    List<StockEntity> findByMarket(@Param("market") com.fineasy.entity.Market market,
                                   org.springframework.data.domain.Pageable pageable);

    @Query("SELECT s FROM StockEntity s WHERE s.isActive = true AND s.market IN :markets ORDER BY s.id ASC")
    List<StockEntity> findByMarkets(@Param("markets") java.util.List<com.fineasy.entity.Market> markets,
                                    org.springframework.data.domain.Pageable pageable);

    @Query("SELECT s FROM StockEntity s WHERE s.isActive = true AND s.market IN :markets ORDER BY s.id ASC")
    List<StockEntity> findAllByMarkets(@Param("markets") java.util.List<com.fineasy.entity.Market> markets);

    @Query("SELECT s FROM StockEntity s WHERE s.isActive = true AND s.sector = :sector ORDER BY s.id ASC")
    List<StockEntity> findBySector(@Param("sector") String sector);
}
