package com.fineasy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.dto.response.NewsAnalysisResponse;
import com.fineasy.dto.response.NewsArticleResponse;
import com.fineasy.dto.response.NewsCountResponse;
import com.fineasy.dto.response.SentimentTrendResponse;
import com.fineasy.dto.response.StockNewsSummaryResponse;
import com.fineasy.dto.response.WatchlistNewsResponse;
import com.fineasy.entity.BokTermEntity;
import com.fineasy.entity.NewsArticleAnalysisEntity;
import com.fineasy.entity.NewsArticleEntity;
import com.fineasy.entity.Sentiment;
import com.fineasy.exception.AiServiceUnavailableException;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.external.openai.OpenAiClient;
import com.fineasy.external.openai.OpenAiPromptBuilder;
import com.fineasy.repository.NewsArticleAnalysisRepository;
import com.fineasy.repository.NewsArticleRepository;
import com.fineasy.repository.StockRepository;
import com.fineasy.dto.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private static final Duration NEWS_ANALYSIS_CACHE_TTL = Duration.ofHours(24);

    private static final Duration STOCK_SUMMARY_CACHE_TTL = Duration.ofHours(1);

    private static final String NEWS_ANALYSIS_CACHE_PREFIX = "news:analysis:";

    private static final String STOCK_SUMMARY_CACHE_PREFIX = "news:stock-summary:";

    private static final int MAX_TOKENS_NEWS_ANALYSIS = 600;

    private static final int MAX_TOKENS_STOCK_SUMMARY = 300;

    private static final int MIN_TAGGED_NEWS_FOR_SKIP_FALLBACK = 3;

    private final NewsArticleRepository newsArticleRepository;

    private final NewsArticleAnalysisRepository newsArticleAnalysisRepository;

    private final StockRepository stockRepository;

    private final ObjectMapper objectMapper;

    private final OpenAiClient openAiClient;

    private final OpenAiPromptBuilder promptBuilder;

    private final BokTermService bokTermService;

    private final RedisCacheHelper redisCacheHelper;

    public NewsService(NewsArticleRepository newsArticleRepository,
                       NewsArticleAnalysisRepository newsArticleAnalysisRepository,
                       StockRepository stockRepository,
                       ObjectMapper objectMapper,
                       @Autowired(required = false) OpenAiClient openAiClient,
                       @Autowired(required = false) OpenAiPromptBuilder promptBuilder,
                       BokTermService bokTermService,
                       RedisCacheHelper redisCacheHelper) {
        this.newsArticleRepository = newsArticleRepository;
        this.newsArticleAnalysisRepository = newsArticleAnalysisRepository;
        this.stockRepository = stockRepository;
        this.objectMapper = objectMapper;
        this.openAiClient = openAiClient;
        this.promptBuilder = promptBuilder;
        this.bokTermService = bokTermService;
        this.redisCacheHelper = redisCacheHelper;
    }

    public PageResponse<NewsArticleResponse> getNews(int page, int size,
                                                     Sentiment sentiment,
                                                     String stockCode) {
        PageRequest pageable = PageRequest.of(page, size);

        List<NewsArticleEntity> articles;
        long totalElements;

        if (stockCode != null && !stockCode.isBlank()) {
            articles = newsArticleRepository.findByStockCode(stockCode, pageable);
            totalElements = newsArticleRepository.countByStockCode(stockCode);
        } else {
            totalElements = newsArticleRepository.countByFilter(sentiment);
            if (sentiment != null) {
                Page<NewsArticleEntity> result = newsArticleRepository.findBySentiment(sentiment, pageable);
                articles = result.getContent();
            } else {
                Page<NewsArticleEntity> result = newsArticleRepository.findAllOrderByPublishedAtDesc(pageable);
                articles = result.getContent();
            }
        }

        List<NewsArticleResponse> content = articles.stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.of(content, page, size, totalElements);
    }

    public NewsArticleResponse getNewsById(long newsId) {
        NewsArticleEntity article = newsArticleRepository.findById(newsId)
                .orElseThrow(() -> new EntityNotFoundException("NewsArticle", newsId));
        return toResponse(article);
    }

    public PageResponse<NewsArticleResponse> getMacroNews(int page, int size) {
        Page<NewsArticleEntity> result = newsArticleRepository
                .findAllOrderByPublishedAtDesc(PageRequest.of(page, size));
        List<NewsArticleResponse> content = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(content, page, size, content.size());
    }

    public List<NewsArticleResponse> getNewsByStockCode(String stockCode, int limit) {

        List<NewsArticleEntity> tagged = newsArticleRepository
                .findByStockCode(stockCode, PageRequest.of(0, limit));
        if (!tagged.isEmpty()) {
            return tagged.stream().map(this::toResponse).toList();
        }

        return stockRepository.findByStockCode(stockCode)
                .map(stock -> newsArticleRepository
                        .findByTitleContaining(stock.getStockName(), stockCode, PageRequest.of(0, limit))
                        .stream().map(this::toResponse).toList())
                .orElse(List.of());
    }

    public List<String> getRecentNewsTitles(String stockCode, int limit) {
        try {

            List<String> tagged = newsArticleRepository
                    .findByStockCode(stockCode, PageRequest.of(0, limit))
                    .stream()
                    .map(NewsArticleEntity::getTitle)
                    .toList();

            if (tagged.size() >= MIN_TAGGED_NEWS_FOR_SKIP_FALLBACK) {
                return tagged;
            }

            return stockRepository.findByStockCode(stockCode)
                    .map(stock -> {
                        String nameKeyword = stock.getStockName().replace("전자", "").replace("그룹", "");
                        String sectorKeyword = stock.getSector() != null ? stock.getSector() : nameKeyword;
                        List<String> keywordResults = newsArticleRepository
                                .findByTitleContaining(nameKeyword, sectorKeyword, PageRequest.of(0, limit))
                                .stream()
                                .map(NewsArticleEntity::getTitle)
                                .toList();
                        return !keywordResults.isEmpty() ? keywordResults : tagged;
                    })
                    .orElse(tagged);
        } catch (Exception e) {
            log.warn("Failed to fetch news titles for stock {}: {}", stockCode, e.getMessage());
            return List.of();
        }
    }

    public Double getAverageSentimentScore(String stockCode, int limit) {
        List<NewsArticleEntity> recentNews = newsArticleRepository
                .findByStockCode(stockCode, PageRequest.of(0, limit));

        if (recentNews.isEmpty()) {
            return null;
        }

        return recentNews.stream()
                .filter(n -> n.getSentimentScore() != null)
                .mapToDouble(NewsArticleEntity::getSentimentScore)
                .average()
                .orElse(0.0);
    }

    public List<WatchlistNewsResponse> getWatchlistNews(List<String> stockCodes, int size) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return List.of();
        }

        List<NewsArticleEntity> articles = newsArticleRepository
                .findByStockCodesIn(stockCodes, PageRequest.of(0, size));

        return articles.stream()
                .map(this::toWatchlistNewsResponse)
                .toList();
    }

    public NewsCountResponse getLatestNewsCount(LocalDateTime since) {
        long count = newsArticleRepository.countNewsSince(since);
        return new NewsCountResponse(count, since);
    }

    public StockNewsSummaryResponse getStockNewsSummary(String stockCode) {
        String cacheKey = STOCK_SUMMARY_CACHE_PREFIX + stockCode;

        StockNewsSummaryResponse cached = redisCacheHelper.getFromCache(
                cacheKey, StockNewsSummaryResponse.class);
        if (cached != null) {
            log.debug("Stock news summary cache hit for stockCode: {}", stockCode);
            return cached;
        }

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<NewsArticleEntity> recentNews = newsArticleRepository
                .findByStockCodeSince(stockCode, since);

        if (recentNews.isEmpty()) {
            StockNewsSummaryResponse empty = StockNewsSummaryResponse.empty(stockCode);
            redisCacheHelper.putToCache(cacheKey, empty, STOCK_SUMMARY_CACHE_TTL);
            return empty;
        }

        if (openAiClient == null || promptBuilder == null) {
            log.warn("OpenAI not configured, returning empty stock news summary");
            return StockNewsSummaryResponse.empty(stockCode);
        }

        try {
            List<String> titles = recentNews.stream()
                    .map(NewsArticleEntity::getTitle)
                    .toList();

            String stockName = stockRepository.findByStockCode(stockCode)
                    .map(stock -> stock.getStockName())
                    .orElse(stockCode);

            String systemPrompt = promptBuilder.getStockNewsSummarySystemPrompt();
            String userPrompt = promptBuilder.buildStockNewsSummaryPrompt(
                    stockName, stockCode, titles);

            String aiResponse = openAiClient.chat(systemPrompt, userPrompt, MAX_TOKENS_STOCK_SUMMARY);
            String summary = parseSummaryResponse(aiResponse);

            StockNewsSummaryResponse response = new StockNewsSummaryResponse(
                    stockCode, summary, recentNews.size(),
                    LocalDateTime.now(), StockNewsSummaryResponse.AI_DISCLAIMER);

            redisCacheHelper.putToCache(cacheKey, response, STOCK_SUMMARY_CACHE_TTL);
            return response;
        } catch (Exception e) {
            log.error("Failed to generate stock news summary for {}: {}",
                    stockCode, e.getMessage(), e);
            return StockNewsSummaryResponse.empty(stockCode);
        }
    }

    public SentimentTrendResponse getSentimentTrend(String stockCode, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<NewsArticleEntity> articles = newsArticleRepository
                .findByStockCodeWithSentimentSince(stockCode, since);

        // Group articles by date
        Map<LocalDate, List<NewsArticleEntity>> grouped = articles.stream()
                .filter(a -> a.getPublishedAt() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getPublishedAt().toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()));

        List<SentimentTrendResponse.DailySentiment> trend = grouped.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<NewsArticleEntity> dayArticles = entry.getValue();

                    double avgScore = dayArticles.stream()
                            .mapToDouble(a -> a.getSentimentScore() != null ? a.getSentimentScore() : 0.5)
                            .average()
                            .orElse(0.5);

                    int positive = 0, negative = 0, neutral = 0;
                    for (NewsArticleEntity a : dayArticles) {
                        if (a.getSentiment() != null) {
                            switch (a.getSentiment()) {
                                case POSITIVE -> positive++;
                                case NEGATIVE -> negative++;
                                case NEUTRAL -> neutral++;
                            }
                        } else {
                            neutral++;
                        }
                    }

                    return new SentimentTrendResponse.DailySentiment(
                            date, Math.round(avgScore * 100.0) / 100.0,
                            dayArticles.size(), positive, negative, neutral);
                })
                .toList();

        return new SentimentTrendResponse(stockCode, days, trend);
    }

    private String parseSummaryResponse(String aiResponse) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(aiResponse);
            return root.path("summary").asText("요약을 생성할 수 없습니다.");
        } catch (Exception e) {
            log.error("Failed to parse stock summary response: {}", aiResponse, e);
            return "요약을 생성할 수 없습니다.";
        }
    }

    @Transactional
    public NewsAnalysisResponse getNewsAnalysis(long newsId) {
        NewsArticleEntity article = newsArticleRepository.findById(newsId)
                .orElseThrow(() -> new EntityNotFoundException("NewsArticle", newsId));

        String cacheKey = NEWS_ANALYSIS_CACHE_PREFIX + newsId;

        NewsAnalysisResponse cached = redisCacheHelper.getFromCache(cacheKey, NewsAnalysisResponse.class);
        if (cached != null) {
            log.debug("News analysis cache hit (Redis) for newsId: {}", newsId);
            return cached;
        }

        Optional<NewsArticleAnalysisEntity> dbAnalysis =
                newsArticleAnalysisRepository.findByNewsArticleId(newsId);
        if (dbAnalysis.isPresent()) {
            log.debug("News analysis cache hit (DB) for newsId: {}", newsId);
            NewsAnalysisResponse fromDb = mapAnalysisEntityToResponse(article, dbAnalysis.get());
            redisCacheHelper.putToCache(cacheKey, fromDb, NEWS_ANALYSIS_CACHE_TTL);
            return fromDb;
        }

        log.info("News analysis cache miss - generating via AI for newsId: {}", newsId);
        return generateNewsAnalysis(article, cacheKey);
    }

    private NewsAnalysisResponse generateNewsAnalysis(NewsArticleEntity article, String cacheKey) {
        if (openAiClient == null || promptBuilder == null) {
            throw new AiServiceUnavailableException(
                    "AI analysis service is not configured. Please set openai.api-key.");
        }

        try {
            String systemPrompt = promptBuilder.getNewsAnalysisSystemPrompt();

            List<BokTermEntity> relatedTerms = List.of();
            try {
                relatedTerms = bokTermService.findRelatedTerms(article.getTitle());
                if (!relatedTerms.isEmpty()) {
                    log.debug("Found {} related BOK terms for newsId {}: {}",
                            relatedTerms.size(), article.getId(),
                            relatedTerms.stream().map(BokTermEntity::getTerm).toList());
                }
            } catch (Exception e) {
                log.warn("Failed to find related BOK terms for newsId {}: {}",
                        article.getId(), e.getMessage());
            }

            String userPrompt;
            if (!relatedTerms.isEmpty()) {
                userPrompt = promptBuilder.buildNewsAnalysisPromptWithTerms(
                        article.getTitle(), article.getContent(),
                        article.getSourceName(), relatedTerms);
            } else {
                userPrompt = promptBuilder.buildNewsAnalysisPrompt(
                        article.getTitle(), article.getContent(), article.getSourceName());
            }

            String aiResponse = openAiClient.chat(systemPrompt, userPrompt, MAX_TOKENS_NEWS_ANALYSIS);
            NewsAnalysisResponse response = parseNewsAnalysisResponse(article, aiResponse);

            saveAnalysisToDb(article, response);

            redisCacheHelper.putToCache(cacheKey, response, NEWS_ANALYSIS_CACHE_TTL);

            return response;
        } catch (AiServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate news analysis for newsId {}: {}",
                    article.getId(), e.getMessage(), e);
            throw new AiServiceUnavailableException(
                    "AI news analysis service is temporarily unavailable. Please try again later.", e);
        }
    }

    private NewsAnalysisResponse parseNewsAnalysisResponse(NewsArticleEntity article, String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            String summary = root.path("summary").asText("뉴스 요약을 생성할 수 없습니다.");
            String marketImpact = root.path("marketImpact").asText("시장 영향을 분석할 수 없습니다.");

            List<String> relatedStocks = List.of();
            if (root.has("relatedStocks") && root.get("relatedStocks").isArray()) {
                relatedStocks = objectMapper.convertValue(root.get("relatedStocks"),
                        new TypeReference<List<String>>() {});
            }

            Sentiment sentiment;
            if (article.getSentiment() != null) {
                sentiment = article.getSentiment();
            } else {
                String sentimentStr = root.path("sentiment").asText("NEUTRAL").toUpperCase();
                try {
                    sentiment = Sentiment.valueOf(sentimentStr);
                } catch (IllegalArgumentException e) {
                    sentiment = Sentiment.NEUTRAL;
                }
            }

            String keyTakeaway = root.path("keyTakeaway").asText("추가 분석이 필요합니다.");

            return new NewsAnalysisResponse(
                    article.getId(),
                    article.getTitle(),
                    article.getSourceName(),
                    article.getPublishedAt(),
                    new NewsAnalysisResponse.Analysis(
                            summary, marketImpact, relatedStocks, sentiment, keyTakeaway
                    ),
                    NewsAnalysisResponse.AI_DISCLAIMER
            );
        } catch (Exception e) {
            log.error("Failed to parse AI news analysis response for newsId {}: {}",
                    article.getId(), aiResponse, e);
            throw new AiServiceUnavailableException(
                    "Failed to parse AI analysis response. Please try again later.", e);
        }
    }

    private void saveAnalysisToDb(NewsArticleEntity article, NewsAnalysisResponse response) {
        try {
            String relatedStocksJson = objectMapper.writeValueAsString(
                    response.analysis().relatedStocks());

            NewsArticleAnalysisEntity entity = new NewsArticleAnalysisEntity(
                    null,
                    article,
                    response.analysis().summary(),
                    response.analysis().marketImpact(),
                    relatedStocksJson,
                    response.analysis().sentiment(),
                    response.analysis().keyTakeaway(),
                    LocalDateTime.now()
            );

            newsArticleAnalysisRepository.save(entity);
            log.debug("Saved news analysis to DB for newsId: {}", article.getId());
        } catch (Exception e) {
            log.error("Failed to save news analysis to DB for newsId: {}", article.getId(), e);
        }
    }

    private NewsAnalysisResponse mapAnalysisEntityToResponse(NewsArticleEntity article,
                                                              NewsArticleAnalysisEntity analysis) {
        List<String> relatedStocks = redisCacheHelper.parseJsonList(analysis.getRelatedStocks());

        Sentiment sentiment = article.getSentiment() != null
                ? article.getSentiment() : analysis.getSentiment();

        return new NewsAnalysisResponse(
                article.getId(),
                article.getTitle(),
                article.getSourceName(),
                article.getPublishedAt(),
                new NewsAnalysisResponse.Analysis(
                        analysis.getSummary(),
                        analysis.getMarketImpact(),
                        relatedStocks,
                        sentiment,
                        analysis.getKeyTakeaway()
                ),
                NewsAnalysisResponse.AI_DISCLAIMER
        );
    }

    private NewsArticleResponse toResponse(NewsArticleEntity article) {
        return new NewsArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getOriginalUrl(),
                article.getSourceName(),
                article.getPublishedAt(),
                article.getSentiment(),
                article.getSentimentScore() != null ? article.getSentimentScore() : 0.5
        );
    }

    private WatchlistNewsResponse toWatchlistNewsResponse(NewsArticleEntity article) {
        List<String> stockTags = article.getTaggedStocks().stream()
                .map(stock -> stock.getStockName())
                .toList();

        return new WatchlistNewsResponse(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getOriginalUrl(),
                article.getSourceName(),
                article.getPublishedAt(),
                article.getSentiment(),
                article.getSentimentScore() != null ? article.getSentimentScore() : 0.5,
                stockTags
        );
    }
}
