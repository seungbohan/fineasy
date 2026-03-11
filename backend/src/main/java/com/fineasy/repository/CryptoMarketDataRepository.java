package com.fineasy.repository;

import com.fineasy.entity.CryptoMarketDataEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CryptoMarketDataRepository extends JpaRepository<CryptoMarketDataEntity, Long> {

    @Query("SELECT c FROM CryptoMarketDataEntity c WHERE c.symbol = :symbol " +
            "ORDER BY c.recordedAt DESC")
    List<CryptoMarketDataEntity> findBySymbolOrderByRecordedAtDesc(
            @Param("symbol") String symbol, Pageable pageable);

    default Optional<CryptoMarketDataEntity> findLatestBySymbol(String symbol) {
        List<CryptoMarketDataEntity> result = findBySymbolOrderByRecordedAtDesc(
                symbol, org.springframework.data.domain.PageRequest.of(0, 1));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Query("SELECT c FROM CryptoMarketDataEntity c WHERE c.recordedAt = " +
            "(SELECT MAX(c2.recordedAt) FROM CryptoMarketDataEntity c2 " +
            "WHERE c2.symbol = c.symbol)")
    List<CryptoMarketDataEntity> findAllLatest();
}
