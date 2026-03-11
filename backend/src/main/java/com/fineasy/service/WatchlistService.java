package com.fineasy.service;

import com.fineasy.dto.response.WatchlistBriefingResponse;
import com.fineasy.dto.response.WatchlistResponse;
import com.fineasy.entity.StockEntity;
import com.fineasy.entity.StockPriceEntity;
import com.fineasy.entity.WatchlistEntity;
import com.fineasy.exception.AiServiceUnavailableException;
import com.fineasy.exception.DuplicateEntityException;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.exception.WatchlistLimitExceededException;
import com.fineasy.external.openai.OpenAiClient;
import com.fineasy.repository.StockPriceRepository;
import com.fineasy.repository.StockRepository;
import com.fineasy.repository.UserRepository;
import com.fineasy.repository.WatchlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WatchlistService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistService.class);

    private static final int MAX_WATCHLIST_SIZE = 30;

    private static final int MAX_BRIEFING_STOCKS = 10;

    private static final int NEWS_TITLES_PER_STOCK = 3;

    private static final int MAX_TOKENS_BRIEFING = 400;

    private static final String BRIEFING_CACHE_PREFIX = "watchlist:briefing:";

    private final WatchlistRepository watchlistRepository;
    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final UserRepository userRepository;
    private final NewsService newsService;
    private final RedisCacheHelper redisCacheHelper;
    private final OpenAiClient openAiClient;

    public WatchlistService(WatchlistRepository watchlistRepository,
                            StockRepository stockRepository,
                            StockPriceRepository stockPriceRepository,
                            UserRepository userRepository,
                            NewsService newsService,
                            RedisCacheHelper redisCacheHelper,
                            @Autowired(required = false) OpenAiClient openAiClient) {
        this.watchlistRepository = watchlistRepository;
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.userRepository = userRepository;
        this.newsService = newsService;
        this.redisCacheHelper = redisCacheHelper;
        this.openAiClient = openAiClient;
    }

    public List<WatchlistResponse> getWatchlist(long userId) {
        List<WatchlistEntity> items = watchlistRepository.findByUserId(userId);
        if (items.isEmpty()) {
            return List.of();
        }

        List<Long> stockIds = items.stream()
                .map(item -> item.getStock().getId())
                .toList();
        java.time.LocalDate since = java.time.LocalDate.now().minusDays(7);
        List<StockPriceEntity> allPrices = stockPriceRepository.findRecentByStockIds(stockIds, since);

        java.util.Map<Long, StockPriceEntity> latestPriceByStockId = new java.util.LinkedHashMap<>();
        for (StockPriceEntity sp : allPrices) {
            latestPriceByStockId.putIfAbsent(sp.getStock().getId(), sp);
        }

        return items.stream()
                .map(item -> {
                    StockEntity stock = item.getStock();
                    StockPriceEntity latestPrice = latestPriceByStockId.get(stock.getId());

                    BigDecimal currentPrice;
                    BigDecimal changeAmount;
                    double changeRate;

                    if (latestPrice != null) {
                        currentPrice = latestPrice.getClosePrice();
                        BigDecimal openPrice = latestPrice.getOpenPrice();
                        changeAmount = currentPrice.subtract(openPrice);
                        changeRate = openPrice.compareTo(BigDecimal.ZERO) > 0
                                ? changeAmount.doubleValue() / openPrice.doubleValue() * 100
                                : 0.0;
                    } else {
                        currentPrice = BigDecimal.ZERO;
                        changeAmount = BigDecimal.ZERO;
                        changeRate = 0.0;
                    }

                    return new WatchlistResponse(
                            item.getId(),
                            stock.getStockCode(),
                            stock.getStockName(),
                            currentPrice,
                            changeAmount,
                            changeRate,
                            item.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public void addToWatchlist(long userId, String stockCode) {
        StockEntity stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new EntityNotFoundException("Stock", stockCode));

        if (watchlistRepository.existsByUserIdAndStockId(userId, stock.getId())) {
            throw new DuplicateEntityException("WatchlistItem", "stockCode", stockCode);
        }

        if (watchlistRepository.countByUserId(userId) >= MAX_WATCHLIST_SIZE) {
            throw new WatchlistLimitExceededException();
        }

        WatchlistEntity item = new WatchlistEntity(
                null,
                userRepository.getReferenceById(userId),
                stock,
                LocalDateTime.now()
        );
        watchlistRepository.save(item);
    }

    @Transactional
    public void removeFromWatchlist(long userId, String stockCode) {
        StockEntity stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new EntityNotFoundException("Stock", stockCode));

        if (!watchlistRepository.existsByUserIdAndStockId(userId, stock.getId())) {
            throw new EntityNotFoundException("WatchlistItem", stockCode);
        }

        watchlistRepository.deleteByUserIdAndStockId(userId, stock.getId());
    }

    public WatchlistBriefingResponse getWatchlistBriefing(long userId) {
        List<WatchlistEntity> items = watchlistRepository.findByUserId(userId);
        if (items.isEmpty()) {
            return null;
        }

        String todayStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String cacheKey = BRIEFING_CACHE_PREFIX + userId + ":" + todayStr;

        WatchlistBriefingResponse cached = redisCacheHelper.getFromCache(cacheKey, WatchlistBriefingResponse.class);
        if (cached != null) {
            log.debug("Watchlist briefing cache hit for userId: {}", userId);
            return cached;
        }

        log.info("Watchlist briefing cache miss - generating via AI for userId: {}", userId);
        return generateBriefing(items, cacheKey);
    }

    private WatchlistBriefingResponse generateBriefing(List<WatchlistEntity> items, String cacheKey) {
        if (openAiClient == null) {
            throw new AiServiceUnavailableException(
                    "AI briefing service is not configured. Please set openai.api-key.");
        }

        try {

            List<WatchlistEntity> targetItems = items.size() > MAX_BRIEFING_STOCKS
                    ? items.subList(0, MAX_BRIEFING_STOCKS) : items;

            StringBuilder newsSection = new StringBuilder();
            for (WatchlistEntity item : targetItems) {
                StockEntity stock = item.getStock();
                List<String> titles = newsService.getRecentNewsTitles(
                        stock.getStockCode(), NEWS_TITLES_PER_STOCK);

                if (!titles.isEmpty()) {
                    String titlesFormatted = titles.stream()
                            .map(t -> "\"" + t + "\"")
                            .collect(Collectors.joining(", "));
                    newsSection.append("- ")
                            .append(stock.getStockName())
                            .append("(").append(stock.getStockCode()).append("): ")
                            .append(titlesFormatted)
                            .append("\n");
                }
            }

            if (newsSection.isEmpty()) {
                WatchlistBriefingResponse noNewsResponse = new WatchlistBriefingResponse(
                        "관심종목에 대한 최근 뉴스가 없어 브리핑을 생성할 수 없습니다.",
                        LocalDateTime.now(),
                        WatchlistBriefingResponse.AI_DISCLAIMER
                );
                cacheBriefing(cacheKey, noNewsResponse);
                return noNewsResponse;
            }

            String systemPrompt = "당신은 금융 뉴스 분석 전문가입니다. " +
                    "사용자의 관심종목별 최근 뉴스 제목을 분석하여 200자 이내의 한국어 브리핑을 작성해주세요.\n\n" +
                    "형식: 전반적인 시장 동향을 요약하고, 각 종목별 핵심 포인트를 한 줄로 정리해주세요.\n" +
                    "JSON 형식으로 응답하세요: {\"briefing\": \"브리핑 내용\"}";

            String userPrompt = "관심종목 뉴스:\n" + newsSection;

            String aiResponse = openAiClient.chat(systemPrompt, userPrompt, MAX_TOKENS_BRIEFING);
            String briefingText = extractBriefingText(aiResponse);

            WatchlistBriefingResponse response = new WatchlistBriefingResponse(
                    briefingText,
                    LocalDateTime.now(),
                    WatchlistBriefingResponse.AI_DISCLAIMER
            );

            cacheBriefing(cacheKey, response);
            return response;

        } catch (AiServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate watchlist briefing: {}", e.getMessage(), e);
            throw new AiServiceUnavailableException(
                    "AI briefing service is temporarily unavailable. Please try again later.", e);
        }
    }

    private String extractBriefingText(String aiResponse) {
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(aiResponse);
            return root.path("briefing").asText("브리핑을 생성할 수 없습니다.");
        } catch (Exception e) {
            log.warn("Failed to parse briefing JSON, using raw response: {}", e.getMessage());
            return aiResponse;
        }
    }

    private void cacheBriefing(String cacheKey, WatchlistBriefingResponse response) {
        Duration ttl = Duration.between(LocalDateTime.now(),
                LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT));
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofMinutes(1);
        }
        redisCacheHelper.putToCache(cacheKey, response, ttl);
    }
}
