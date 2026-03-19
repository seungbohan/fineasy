package com.fineasy.repository;

import com.fineasy.entity.ImpactType;
import com.fineasy.entity.NewsStockTagEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NewsStockTagRepository extends JpaRepository<NewsStockTagEntity, Long> {

    Optional<NewsStockTagEntity> findByNewsArticleIdAndStockId(Long newsArticleId, Long stockId);

    @Query("SELECT t FROM NewsStockTagEntity t JOIN FETCH t.newsArticle n " +
            "WHERE t.stock.stockCode = :stockCode " +
            "AND t.impactType = :impactType " +
            "AND n.publishedAt >= :since " +
            "ORDER BY t.relevanceScore DESC NULLS LAST, n.publishedAt DESC")
    List<NewsStockTagEntity> findByStockCodeAndImpactType(
            @Param("stockCode") String stockCode,
            @Param("impactType") ImpactType impactType,
            @Param("since") LocalDateTime since,
            Pageable pageable);

    @Query("SELECT t FROM NewsStockTagEntity t JOIN FETCH t.newsArticle n " +
            "WHERE t.stock.stockCode = :stockCode " +
            "AND t.impactType IN :impactTypes " +
            "AND n.publishedAt >= :since " +
            "ORDER BY t.relevanceScore DESC NULLS LAST, n.publishedAt DESC")
    List<NewsStockTagEntity> findByStockCodeAndImpactTypeIn(
            @Param("stockCode") String stockCode,
            @Param("impactTypes") Collection<ImpactType> impactTypes,
            @Param("since") LocalDateTime since,
            Pageable pageable);

    @Query("SELECT t FROM NewsStockTagEntity t JOIN FETCH t.newsArticle n " +
            "WHERE t.stock.stockCode = :stockCode " +
            "AND n.publishedAt >= :since " +
            "ORDER BY t.relevanceScore DESC NULLS LAST, n.publishedAt DESC")
    List<NewsStockTagEntity> findByStockCodeSince(
            @Param("stockCode") String stockCode,
            @Param("since") LocalDateTime since,
            Pageable pageable);

    boolean existsByNewsArticleIdAndStockId(Long newsArticleId, Long stockId);

    /**
     * Find co-occurring stock pairs from the last N days for relationship inference.
     * Returns [stockId1, stockId2, coCount] where stockId1 < stockId2.
     */
    @Query(value = """
            SELECT t1.stock_id, t2.stock_id, COUNT(*) as co_count
            FROM news_stock_tags_v2 t1
            JOIN news_stock_tags_v2 t2 ON t1.news_article_id = t2.news_article_id
            JOIN news_articles na ON na.id = t1.news_article_id
            WHERE t1.stock_id < t2.stock_id
              AND na.published_at >= :since
            GROUP BY t1.stock_id, t2.stock_id
            HAVING COUNT(*) >= :minCount
            ORDER BY co_count DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findCoOccurringStocks(
            @Param("since") LocalDateTime since,
            @Param("minCount") int minCount,
            @Param("limit") int limit);
}
