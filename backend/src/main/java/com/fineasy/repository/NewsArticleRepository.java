package com.fineasy.repository;

import com.fineasy.entity.Sentiment;
import com.fineasy.entity.NewsArticleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface NewsArticleRepository extends JpaRepository<NewsArticleEntity, Long> {

    @Query("SELECT n FROM NewsArticleEntity n ORDER BY n.publishedAt DESC")
    Page<NewsArticleEntity> findAllOrderByPublishedAtDesc(Pageable pageable);

    @Query("SELECT n FROM NewsArticleEntity n WHERE n.sentiment = :sentiment " +
            "ORDER BY n.publishedAt DESC")
    Page<NewsArticleEntity> findBySentiment(
            @Param("sentiment") Sentiment sentiment, Pageable pageable);

    @Query("SELECT n FROM NewsArticleEntity n JOIN n.taggedStocks s " +
            "WHERE s.stockCode = :stockCode ORDER BY n.publishedAt DESC")
    List<NewsArticleEntity> findByStockCode(
            @Param("stockCode") String stockCode, Pageable pageable);

    @Query("SELECT n FROM NewsArticleEntity n WHERE " +
            "n.title LIKE %:keyword1% OR n.title LIKE %:keyword2% " +
            "ORDER BY n.publishedAt DESC")
    List<NewsArticleEntity> findByTitleContaining(
            @Param("keyword1") String keyword1,
            @Param("keyword2") String keyword2,
            Pageable pageable);

    @Query("SELECT COUNT(n) FROM NewsArticleEntity n WHERE " +
            "(:sentiment IS NULL OR n.sentiment = :sentiment)")
    long countByFilter(@Param("sentiment") Sentiment sentiment);

    boolean existsByOriginalUrl(String originalUrl);

    boolean existsByTitle(String title);

    @Query("SELECT n.originalUrl FROM NewsArticleEntity n WHERE n.originalUrl IN :urls")
    Set<String> findExistingUrls(@Param("urls") Collection<String> urls);

    @Query("SELECT n.title FROM NewsArticleEntity n WHERE n.title IN :titles")
    Set<String> findExistingTitles(@Param("titles") Collection<String> titles);

    @Query("SELECT COUNT(DISTINCT n.id) FROM NewsArticleEntity n JOIN n.taggedStocks s " +
            "WHERE s.stockCode = :stockCode")
    long countByStockCode(@Param("stockCode") String stockCode);

    @Query("SELECT n FROM NewsArticleEntity n WHERE n.sentiment IS NULL ORDER BY n.publishedAt DESC")
    List<NewsArticleEntity> findBySentimentIsNull(Pageable pageable);

    @Query("SELECT n FROM NewsArticleEntity n WHERE n.sentiment IS NULL AND n.createdAt >= :since ORDER BY n.publishedAt DESC")
    List<NewsArticleEntity> findBySentimentIsNullAndCreatedAtAfter(
            @Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT DISTINCT n FROM NewsArticleEntity n JOIN n.taggedStocks s " +
            "WHERE s.stockCode IN :stockCodes ORDER BY n.publishedAt DESC")
    List<NewsArticleEntity> findByStockCodesIn(
            @Param("stockCodes") Collection<String> stockCodes, Pageable pageable);

    @Query("SELECT n FROM NewsArticleEntity n JOIN n.taggedStocks s " +
            "WHERE s.stockCode = :stockCode AND n.publishedAt >= :since " +
            "ORDER BY n.publishedAt DESC")
    List<NewsArticleEntity> findByStockCodeSince(
            @Param("stockCode") String stockCode,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(n) FROM NewsArticleEntity n WHERE n.publishedAt >= :since")
    long countNewsSince(@Param("since") LocalDateTime since);

    @Query("SELECT n FROM NewsArticleEntity n WHERE " +
            "LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY n.publishedAt DESC")
    List<NewsArticleEntity> findByTitleContainingKeyword(
            @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT n FROM NewsArticleEntity n JOIN n.taggedStocks s " +
            "WHERE s.stockCode = :stockCode AND n.publishedAt >= :since " +
            "AND n.sentimentScore IS NOT NULL " +
            "ORDER BY n.publishedAt ASC")
    List<NewsArticleEntity> findByStockCodeWithSentimentSince(
            @Param("stockCode") String stockCode,
            @Param("since") LocalDateTime since);
}
