package com.fineasy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fineasy.entity.Market;
import com.fineasy.entity.NewsArticleEntity;
import com.fineasy.entity.StockEntity;
import com.fineasy.external.finnhub.FinnhubApiClient;
import com.fineasy.repository.NewsArticleRepository;
import com.fineasy.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@ConditionalOnExpression("!'${finnhub.api.key:}'.isEmpty()")
public class FinnhubNewsCollectorService {

    private static final Logger log = LoggerFactory.getLogger(FinnhubNewsCollectorService.class);

    private static final String SOURCE_NAME = "Finnhub";
    private static final int COLLECTION_DAYS_BACK = 3;

    private final FinnhubApiClient finnhubApiClient;
    private final NewsArticleRepository newsArticleRepository;
    private final StockRepository stockRepository;

    public FinnhubNewsCollectorService(FinnhubApiClient finnhubApiClient,
                                        NewsArticleRepository newsArticleRepository,
                                        StockRepository stockRepository) {
        this.finnhubApiClient = finnhubApiClient;
        this.newsArticleRepository = newsArticleRepository;
        this.stockRepository = stockRepository;
    }

    /**
     * Collect news for a list of overseas stock symbols via Finnhub.
     * Returns the total count of newly saved articles.
     */
    @Transactional
    public int collectNewsForSymbols(List<String> symbols) {
        int totalSaved = 0;
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(COLLECTION_DAYS_BACK);

        for (String symbol : symbols) {
            try {
                int saved = collectNewsForSymbol(symbol, from, to);
                totalSaved += saved;

                // Finnhub free tier: 60 calls/min — 1 second delay keeps us safely under limit
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Finnhub news collection interrupted at symbol={}", symbol);
                break;
            } catch (Exception e) {
                log.error("Failed to collect Finnhub news for symbol={}: {}", symbol, e.getMessage());
            }
        }

        return totalSaved;
    }

    private int collectNewsForSymbol(String symbol, LocalDate from, LocalDate to) {
        JsonNode newsArray = finnhubApiClient.fetchCompanyNews(symbol, from, to);
        if (newsArray == null || !newsArray.isArray() || newsArray.isEmpty()) {
            return 0;
        }

        // Pre-fetch existing URLs for deduplication
        List<String> candidateUrls = new ArrayList<>();
        for (JsonNode item : newsArray) {
            String url = item.path("url").asText("");
            if (!url.isBlank()) {
                candidateUrls.add(truncate(url, 1000));
            }
        }
        Set<String> existingUrls = newsArticleRepository.findExistingUrls(candidateUrls);

        // Resolve the StockEntity for tagging
        Optional<StockEntity> stockOpt = stockRepository.findByStockCode(symbol);

        int saved = 0;
        for (JsonNode item : newsArray) {
            try {
                String url = truncate(item.path("url").asText(""), 1000);
                if (url.isBlank() || existingUrls.contains(url)) {
                    continue;
                }

                String headline = truncate(item.path("headline").asText(""), 500);
                if (headline.isBlank()) continue;

                String summary = item.path("summary").asText(null);
                String source = truncate(item.path("source").asText(SOURCE_NAME), 100);
                long datetime = item.path("datetime").asLong(0);

                LocalDateTime publishedAt = datetime > 0
                        ? LocalDateTime.ofInstant(Instant.ofEpochSecond(datetime), ZoneId.systemDefault())
                        : LocalDateTime.now();

                List<StockEntity> taggedStocks = stockOpt.map(List::of).orElse(List.of());

                NewsArticleEntity article = new NewsArticleEntity(
                        null,
                        headline,
                        summary,
                        url,
                        source,
                        publishedAt,
                        null,
                        null,
                        null,
                        taggedStocks
                );

                newsArticleRepository.save(article);
                existingUrls.add(url); // Prevent duplicates within same batch
                saved++;
            } catch (Exception e) {
                log.warn("Failed to save Finnhub news article: {}", e.getMessage());
            }
        }

        if (saved > 0) {
            log.info("Finnhub: saved {} new articles for symbol={}", saved, symbol);
        }
        return saved;
    }

    /**
     * Get ALL overseas stock symbols ordered by market cap (descending).
     * Larger companies are collected first so that if the process is interrupted,
     * the most important stocks already have their news.
     */
    public List<String> getOverseasStockSymbols() {
        List<Market> overseasMarkets = List.of(Market.NASDAQ, Market.NYSE, Market.AMEX);

        List<StockEntity> stocks = stockRepository.findOverseasByMarketCap(
                overseasMarkets,
                org.springframework.data.domain.Pageable.unpaged());

        if (stocks.isEmpty()) {
            stocks = stockRepository.findAllByMarkets(overseasMarkets);
        }

        log.info("Selected {} overseas symbols for Finnhub news collection (market-cap ranked)", stocks.size());
        return stocks.stream()
                .map(StockEntity::getStockCode)
                .toList();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
