package com.fineasy.service;

import com.fineasy.entity.RelationType;
import com.fineasy.entity.StockEntity;
import com.fineasy.entity.StockRelationEntity;
import com.fineasy.repository.NewsStockTagRepository;
import com.fineasy.repository.StockRelationRepository;
import com.fineasy.repository.StockRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Infers stock-to-stock relationships from news co-tagging patterns.
 * Runs weekly (Sunday 5 AM) with zero LLM cost — pure SQL analysis.
 */
@Service
public class StockRelationInferenceService {

    private static final Logger log = LoggerFactory.getLogger(StockRelationInferenceService.class);

    private static final int MIN_CO_OCCURRENCE = 3;
    private static final int MAX_RELATIONS = 200;
    private static final int RELATION_VALIDITY_DAYS = 30;

    private final NewsStockTagRepository newsStockTagRepository;
    private final StockRelationRepository stockRelationRepository;
    private final StockRepository stockRepository;
    private final JdbcTemplate jdbcTemplate;

    public StockRelationInferenceService(NewsStockTagRepository newsStockTagRepository,
                                          StockRelationRepository stockRelationRepository,
                                          StockRepository stockRepository,
                                          JdbcTemplate jdbcTemplate) {
        this.newsStockTagRepository = newsStockTagRepository;
        this.stockRelationRepository = stockRelationRepository;
        this.stockRepository = stockRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 0 5 * * SUN")
    @SchedulerLock(name = "inferStockRelations", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    @Transactional
    public void inferRelationsFromCoTagging() {
        log.info("Starting stock relation inference from co-tagging patterns...");

        LocalDateTime since = LocalDateTime.now().minusDays(30);

        List<Object[]> coTags = newsStockTagRepository.findCoOccurringStocks(
                since, MIN_CO_OCCURRENCE, MAX_RELATIONS);

        if (coTags.isEmpty()) {
            log.info("No co-occurring stock pairs found.");
            return;
        }

        // Find max co-occurrence count for normalization
        long maxCoCount = coTags.stream()
                .mapToLong(row -> ((Number) row[2]).longValue())
                .max().orElse(1);

        int created = 0;
        int updated = 0;
        LocalDate validUntil = LocalDate.now().plusDays(RELATION_VALIDITY_DAYS);

        for (Object[] row : coTags) {
            Long stockId1 = ((Number) row[0]).longValue();
            Long stockId2 = ((Number) row[1]).longValue();
            long coCount = ((Number) row[2]).longValue();

            // Normalize weight: 0.3 ~ 1.0 range
            double weight = 0.3 + (0.7 * coCount / maxCoCount);

            // Determine relation type based on stock sectors
            RelationType relationType = inferRelationType(stockId1, stockId2);

            // Create bidirectional relations
            if (upsertRelation(stockId1, stockId2, relationType, weight, validUntil)) {
                created++;
            } else {
                updated++;
            }
            if (upsertRelation(stockId2, stockId1, relationType, weight, validUntil)) {
                created++;
            } else {
                updated++;
            }
        }

        // Clean up expired relations
        int deleted = stockRelationRepository.deleteExpiredRelations(LocalDate.now());

        log.info("Stock relation inference completed: {} created, {} updated, {} expired deleted",
                created, updated, deleted);
    }

    private boolean upsertRelation(Long sourceId, Long targetId, RelationType type,
                                    double weight, LocalDate validUntil) {
        Optional<StockRelationEntity> existing =
                stockRelationRepository.findBySourceStockIdAndTargetStockIdAndRelationType(
                        sourceId, targetId, type);

        if (existing.isPresent()) {
            existing.get().updateWeight(weight);
            existing.get().extendValidity(validUntil);
            stockRelationRepository.save(existing.get());
            return false;
        }

        StockEntity source = stockRepository.findById(sourceId).orElse(null);
        StockEntity target = stockRepository.findById(targetId).orElse(null);
        if (source == null || target == null) return false;

        stockRelationRepository.save(
                new StockRelationEntity(source, target, type, weight, validUntil));
        return true;
    }

    private RelationType inferRelationType(Long stockId1, Long stockId2) {
        StockEntity s1 = stockRepository.findById(stockId1).orElse(null);
        StockEntity s2 = stockRepository.findById(stockId2).orElse(null);

        if (s1 == null || s2 == null) return RelationType.SAME_SECTOR;

        // Same sector → SAME_SECTOR
        if (s1.getSector() != null && s1.getSector().equals(s2.getSector())) {
            return RelationType.SAME_SECTOR;
        }

        // Cross-market co-occurrence likely indicates supply chain
        boolean s1Overseas = isOverseas(s1);
        boolean s2Overseas = isOverseas(s2);
        if (s1Overseas != s2Overseas) {
            return RelationType.SUPPLY_CHAIN;
        }

        return RelationType.SAME_SECTOR;
    }

    private boolean isOverseas(StockEntity stock) {
        return stock.getMarket() == com.fineasy.entity.Market.NASDAQ
                || stock.getMarket() == com.fineasy.entity.Market.NYSE
                || stock.getMarket() == com.fineasy.entity.Market.AMEX;
    }

    /**
     * Find related stock IDs using recursive CTE for multi-hop traversal.
     * Used by AI analysis to expand news search scope.
     */
    public List<Long> findRelatedStockIds(Long stockId, int maxDepth, double minWeight) {
        return jdbcTemplate.queryForList("""
                WITH RECURSIVE related AS (
                    SELECT target_stock_id AS stock_id,
                           weight,
                           1 AS depth
                    FROM stock_relations
                    WHERE source_stock_id = ?
                      AND weight >= ?
                      AND valid_until >= CURRENT_DATE

                    UNION ALL

                    SELECT sr.target_stock_id,
                           sr.weight * r.weight AS weight,
                           r.depth + 1
                    FROM stock_relations sr
                    JOIN related r ON sr.source_stock_id = r.stock_id
                    WHERE r.depth < ?
                      AND sr.weight * r.weight >= 0.3
                      AND sr.valid_until >= CURRENT_DATE
                      AND sr.target_stock_id != ?
                )
                SELECT DISTINCT stock_id
                FROM related
                WHERE stock_id != ?
                ORDER BY weight DESC
                LIMIT 10
                """, Long.class, stockId, minWeight, maxDepth, stockId, stockId);
    }
}
