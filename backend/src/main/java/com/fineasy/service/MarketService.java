package com.fineasy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.dto.response.MarketIndexResponse;
import com.fineasy.dto.response.MarketSummaryResponse;
import com.fineasy.dto.response.StockRankingResponse;
import com.fineasy.entity.Market;
import com.fineasy.entity.MarketIndex;
import com.fineasy.entity.StockEntity;
import com.fineasy.entity.StockPriceEntity;
import com.fineasy.repository.StockPriceRepository;
import com.fineasy.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(readOnly = true)
public class MarketService {

    private static final Logger log = LoggerFactory.getLogger(MarketService.class);

    private static final String MARKET_INDEX_CACHE_KEY = "market:indices";
    private static final Duration MARKET_INDEX_CACHE_TTL = Duration.ofMinutes(5);

    private final MarketDataProvider marketDataProvider;
    private final ObjectMapper objectMapper;
    private final AiMarketSummaryService aiMarketSummaryService;
    private final RedisCacheHelper redisCacheHelper;

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;

    private final Map<String, StockRankingResponse> rankingCache = new ConcurrentHashMap<>();

    public MarketService(MarketDataProvider marketDataProvider,
                          ObjectMapper objectMapper,
                          AiMarketSummaryService aiMarketSummaryService,
                          RedisCacheHelper redisCacheHelper,
                          StockRepository stockRepository,
                          StockPriceRepository stockPriceRepository) {
        this.marketDataProvider = marketDataProvider;
        this.objectMapper = objectMapper;
        this.aiMarketSummaryService = aiMarketSummaryService;
        this.redisCacheHelper = redisCacheHelper;
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
    }

    public MarketIndexResponse getMarketIndices() {
        // Check Redis cache first
        MarketIndexResponse cached = redisCacheHelper.getFromCache(
                MARKET_INDEX_CACHE_KEY, MarketIndexResponse.class);
        if (cached != null) {
            return cached;
        }

        List<MarketIndex> indices = marketDataProvider.getMarketIndices();

        List<MarketIndexResponse.IndexData> indexDataList = indices.stream()
                .map(idx -> new MarketIndexResponse.IndexData(
                        idx.code(),
                        idx.name(),
                        idx.currentValue(),
                        idx.changeAmount(),
                        idx.changeRate(),
                        idx.sparklineData()
                ))
                .toList();

        MarketIndexResponse response = new MarketIndexResponse(indexDataList, Instant.now());

        // Cache for 5 minutes
        redisCacheHelper.putToCache(MARKET_INDEX_CACHE_KEY, response, MARKET_INDEX_CACHE_TTL);

        return response;
    }

    private static final String MARKET_SUMMARY_CACHE_KEY = "market:summary";
    private static final Duration MARKET_SUMMARY_CACHE_TTL = Duration.ofHours(1);

    public MarketSummaryResponse getMarketSummary() {
        MarketSummaryResponse cached = redisCacheHelper.getFromCache(MARKET_SUMMARY_CACHE_KEY, MarketSummaryResponse.class);
        if (cached != null) {
            return cached;
        }

        MarketSummaryResponse response;
        try {
            AiMarketSummaryService.MarketSummaryData aiData = aiMarketSummaryService.generateMarketSummary();
            if (aiData != null) {
                response = new MarketSummaryResponse(
                        aiData.overview(),
                        aiData.sentiment(),
                        aiData.sentimentLabel(),
                        aiData.overview(),
                        aiData.macro(),
                        aiData.news(),
                        aiData.tip(),
                        Instant.now()
                );
                redisCacheHelper.putToCache(MARKET_SUMMARY_CACHE_KEY, response, MARKET_SUMMARY_CACHE_TTL);
                return response;
            }
        } catch (Exception e) {
            log.warn("AI market summary unavailable: {}", e.getMessage());
        }

        String summary = marketDataProvider.getMarketSummary();
        return new MarketSummaryResponse(summary, null, null, null, null, null, null, Instant.now());
    }

    @Cacheable(value = "market-ranking", key = "#type + ':' + #size + ':' + #region", unless = "#result == null")
    public StockRankingResponse getStockRanking(String type, int size, String region) {
        int limitedSize = Math.min(size, 30);

        if ("overseas".equals(region)) {

            try {
                StockRankingResponse overseasResponse = marketDataProvider.getOverseasStockRanking(type, limitedSize);
                if (overseasResponse.stocks() != null && !overseasResponse.stocks().isEmpty()) {
                    log.debug("KIS overseas ranking: {} items for type={}", overseasResponse.stocks().size(), type);
                    return overseasResponse;
                }
            } catch (Exception e) {
                log.warn("KIS overseas ranking API failed for type={}: {}", type, e.getMessage());
            }

            return getDbBasedStockRanking(type, limitedSize,
                    List.of(Market.NASDAQ, Market.NYSE));
        }

        int fetchSize = 30;
        StockRankingResponse response = marketDataProvider.getStockRanking(type, fetchSize);
        boolean hasData = response.stocks() != null && !response.stocks().isEmpty();

        boolean isValidData = hasData && (hasNonZeroChangeRate(response)
                || "volume".equals(type) || "trading_value".equals(type));
        if (isValidData) {
            StockRankingResponse filtered = filterByDirection(response, type);
            log.debug("KIS ranking type={}: {} raw -> {} filtered",
                    type, response.stocks().size(), filtered.stocks().size());
            if (!filtered.stocks().isEmpty()) {

                if ("volume".equals(type)) {
                    filtered = reSortByVolume(filtered);
                } else if ("trading_value".equals(type)) {
                    filtered = reSortByTradingValue(filtered);
                }
                StockRankingResponse result = trimToSize(filtered, limitedSize);

                rankingCache.put(type, result);
                return result;
            }

        }

        StockRankingResponse cached = rankingCache.get(type);
        if (cached != null && !cached.stocks().isEmpty()) {
            log.info("KIS ranking API returned empty for type={}, using cached result", type);
            return trimToSize(cached, limitedSize);
        }

        log.warn("No KIS ranking cache for type={}, falling back to DB", type);
        return getDbBasedStockRanking(type, limitedSize,
                List.of(Market.KRX, Market.KOSDAQ));
    }

    private StockRankingResponse getDbBasedStockRanking(String type, int size, List<Market> markets) {
        List<StockEntity> stocks = stockRepository.findAllByMarkets(markets);
        if (stocks.isEmpty()) {
            return new StockRankingResponse(type, List.of(), Instant.now());
        }

        Map<Long, List<StockPriceEntity>> pricesByStock = fetchRecentPricesByStock(stocks);
        Map<Long, StockEntity> stockById = buildStockLookup(stocks);
        List<StockRankingResponse.RankedStock> ranked = calculateRankedStocks(pricesByStock, stockById);

        filterByDirection(ranked, type);
        sortByType(ranked, type);

        return new StockRankingResponse(type, reRank(ranked, size), Instant.now());
    }

    private Map<Long, List<StockPriceEntity>> fetchRecentPricesByStock(List<StockEntity> stocks) {
        List<Long> stockIds = stocks.stream().map(StockEntity::getId).toList();
        java.time.LocalDate since = java.time.LocalDate.now().minusDays(14);
        List<StockPriceEntity> allPrices = stockPriceRepository.findRecentByStockIds(stockIds, since);

        Map<Long, List<StockPriceEntity>> pricesByStock = new java.util.LinkedHashMap<>();
        for (StockPriceEntity sp : allPrices) {
            pricesByStock.computeIfAbsent(sp.getStock().getId(), k -> new ArrayList<>()).add(sp);
        }
        return pricesByStock;
    }

    private Map<Long, StockEntity> buildStockLookup(List<StockEntity> stocks) {
        Map<Long, StockEntity> stockById = new java.util.HashMap<>();
        for (StockEntity stock : stocks) {
            stockById.put(stock.getId(), stock);
        }
        return stockById;
    }

    private List<StockRankingResponse.RankedStock> calculateRankedStocks(
            Map<Long, List<StockPriceEntity>> pricesByStock,
            Map<Long, StockEntity> stockById) {
        List<StockRankingResponse.RankedStock> ranked = new ArrayList<>();
        for (Map.Entry<Long, List<StockPriceEntity>> entry : pricesByStock.entrySet()) {
            List<StockPriceEntity> prices = entry.getValue();
            if (prices.size() < 2) continue;

            StockEntity stock = stockById.get(entry.getKey());
            if (stock == null) continue;

            BigDecimal currentPrice = prices.get(0).getClosePrice();
            BigDecimal prevPrice = prices.get(1).getClosePrice();
            if (currentPrice == null || prevPrice == null
                    || prevPrice.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal changeAmount = currentPrice.subtract(prevPrice);
            double changeRate = changeAmount.doubleValue() / prevPrice.doubleValue() * 100;
            long volume = prices.get(0).getVolume() != null ? prices.get(0).getVolume() : 0L;
            double tradingValue = currentPrice.doubleValue() * volume;

            ranked.add(new StockRankingResponse.RankedStock(
                    0, stock.getStockCode(), stock.getStockName(),
                    currentPrice.doubleValue(), changeAmount.doubleValue(), changeRate, volume, tradingValue));
        }
        return ranked;
    }

    private void filterByDirection(List<StockRankingResponse.RankedStock> ranked, String type) {
        if ("gainers".equals(type)) {
            ranked.removeIf(s -> s.changeRate() <= 0);
        } else if ("losers".equals(type)) {
            ranked.removeIf(s -> s.changeRate() >= 0);
        }
    }

    private void sortByType(List<StockRankingResponse.RankedStock> ranked, String type) {
        switch (type) {
            case "losers" -> ranked.sort((a, b) -> Double.compare(a.changeRate(), b.changeRate()));
            case "volume" -> ranked.sort((a, b) -> Long.compare(b.volume(), a.volume()));
            case "trading_value" -> ranked.sort((a, b) -> Double.compare(b.tradingValue(), a.tradingValue()));
            default -> ranked.sort((a, b) -> Double.compare(b.changeRate(), a.changeRate()));
        }
    }

    private List<StockRankingResponse.RankedStock> reRank(
            List<StockRankingResponse.RankedStock> stocks, int size) {
        List<StockRankingResponse.RankedStock> result = new ArrayList<>();
        int limit = Math.min(size, stocks.size());
        for (int i = 0; i < limit; i++) {
            var s = stocks.get(i);
            result.add(new StockRankingResponse.RankedStock(
                    i + 1, s.stockCode(), s.stockName(),
                    s.currentPrice(), s.changeAmount(), s.changeRate(), s.volume(), s.tradingValue()));
        }
        return result;
    }

    private boolean hasNonZeroChangeRate(StockRankingResponse response) {
        return response.stocks().stream()
                .anyMatch(s -> s.changeRate() != 0.0);
    }

    private StockRankingResponse filterByDirection(StockRankingResponse response, String type) {
        if (!"gainers".equals(type) && !"losers".equals(type)) {
            return response;
        }

        List<StockRankingResponse.RankedStock> filtered = response.stocks().stream()
                .filter(s -> "gainers".equals(type) ? s.changeRate() > 0 : s.changeRate() < 0)
                .toList();

        return new StockRankingResponse(type, reRank(filtered, filtered.size()), response.updatedAt());
    }

    private StockRankingResponse reSortByVolume(StockRankingResponse response) {
        List<StockRankingResponse.RankedStock> sorted = new ArrayList<>(response.stocks());
        sorted.sort((a, b) -> Long.compare(b.volume(), a.volume()));
        return new StockRankingResponse(response.type(), reRank(sorted, sorted.size()), response.updatedAt());
    }

    private StockRankingResponse reSortByTradingValue(StockRankingResponse response) {
        List<StockRankingResponse.RankedStock> sorted = new ArrayList<>(response.stocks());
        sorted.sort((a, b) -> Double.compare(b.tradingValue(), a.tradingValue()));
        return new StockRankingResponse(response.type(), reRank(sorted, sorted.size()), response.updatedAt());
    }

    private StockRankingResponse trimToSize(StockRankingResponse response, int size) {
        if (response.stocks().size() <= size) {
            return response;
        }
        return new StockRankingResponse(response.type(),
                reRank(response.stocks(), size), response.updatedAt());
    }

}
