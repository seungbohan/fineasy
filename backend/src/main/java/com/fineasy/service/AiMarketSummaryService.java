package com.fineasy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.config.OpenAiConfig;
import com.fineasy.entity.MacroIndicatorEntity;
import com.fineasy.entity.MarketIndex;
import com.fineasy.entity.NewsArticleEntity;
import com.fineasy.external.openai.OpenAiClient;
import com.fineasy.external.openai.OpenAiPromptBuilder;
import com.fineasy.repository.MacroIndicatorRepository;
import com.fineasy.repository.NewsArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AiMarketSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiMarketSummaryService.class);

    private static final Duration AI_SUMMARY_CACHE_TTL = Duration.ofHours(4);

    private static final int MAX_TOKENS_MARKET_SUMMARY = 1200;

    private static final List<String> KEY_MACRO_CODES = List.of(
            "KR_BASE_RATE", "US_FED_FUNDS_RATE", "USD_KRW", "US_10Y_TREASURY",
            "US_VIX", "GOLD", "WTI", "US_DXY", "US_YIELD_SPREAD"
    );

    private static final int MAX_NEWS_FOR_SUMMARY = 15;

    private final MarketDataProvider marketDataProvider;
    private final MacroIndicatorRepository macroIndicatorRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final OpenAiConfig openAiConfig;
    private final ObjectMapper objectMapper;

    private final OpenAiClient openAiClient;
    private final OpenAiPromptBuilder promptBuilder;

    private volatile MarketSummaryData cachedAiSummary;
    private volatile Instant cachedAt;

    public record MarketSummaryData(
            String summary,
            String sentiment,
            String sentimentLabel,
            String overview,
            String macro,
            String news,
            String tip
    ) {}

    public AiMarketSummaryService(MarketDataProvider marketDataProvider,
                                   MacroIndicatorRepository macroIndicatorRepository,
                                   NewsArticleRepository newsArticleRepository,
                                   OpenAiConfig openAiConfig,
                                   ObjectMapper objectMapper,
                                   @Autowired(required = false) OpenAiClient openAiClient,
                                   @Autowired(required = false) OpenAiPromptBuilder promptBuilder) {
        this.marketDataProvider = marketDataProvider;
        this.macroIndicatorRepository = macroIndicatorRepository;
        this.newsArticleRepository = newsArticleRepository;
        this.openAiConfig = openAiConfig;
        this.objectMapper = objectMapper;
        this.openAiClient = openAiClient;
        this.promptBuilder = promptBuilder;
    }

    public MarketSummaryData generateMarketSummary() {
        if (!isAiAvailable()) {
            return null;
        }

        if (cachedAiSummary != null && cachedAt != null
                && Duration.between(cachedAt, Instant.now()).compareTo(AI_SUMMARY_CACHE_TTL) < 0) {
            log.debug("AI market summary cache hit (cached at: {})", cachedAt);
            return cachedAiSummary;
        }

        try {
            List<MarketIndex> indices = marketDataProvider.getMarketIndices();
            List<MacroIndicatorEntity> macroIndicators = loadKeyMacroIndicators();
            List<String> newsTitles = loadRecentNewsTitles();

            String systemPrompt = promptBuilder.getMarketSummarySystemPrompt();
            String userPrompt = promptBuilder.buildMarketSummaryPrompt(indices, macroIndicators, newsTitles);

            String aiResponse = openAiClient.chat(systemPrompt, userPrompt, MAX_TOKENS_MARKET_SUMMARY);
            MarketSummaryData data = parseMarketSummaryResponse(aiResponse);

            if (data != null) {
                cachedAiSummary = data;
                cachedAt = Instant.now();
                log.info("AI market summary generated and cached successfully");
                return data;
            }
        } catch (Exception e) {
            log.warn("Failed to generate AI market summary: {}", e.getMessage());
        }

        return null;
    }

    private boolean isAiAvailable() {
        return openAiClient != null
                && promptBuilder != null
                && openAiConfig != null
                && openAiConfig.getApiKey() != null
                && !openAiConfig.getApiKey().isBlank();
    }

    private MarketSummaryData parseMarketSummaryResponse(String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            String overview = root.path("overview").asText(null);
            if (overview == null || overview.isBlank()) {
                return null;
            }
            return new MarketSummaryData(
                    overview,
                    root.path("sentiment").asText("NEUTRAL"),
                    root.path("sentimentLabel").asText("보합세"),
                    overview,
                    root.path("macro").asText(null),
                    root.path("news").asText(null),
                    root.path("tip").asText(null)
            );
        } catch (Exception e) {
            log.error("Failed to parse AI market summary response: {}", aiResponse, e);
            return null;
        }
    }

    private List<MacroIndicatorEntity> loadKeyMacroIndicators() {
        try {
            return macroIndicatorRepository.findLatestByIndicatorCodes(KEY_MACRO_CODES);
        } catch (Exception e) {
            log.warn("Failed to load macro indicators for market summary: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> loadRecentNewsTitles() {
        try {
            return newsArticleRepository
                    .findAllOrderByPublishedAtDesc(PageRequest.of(0, MAX_NEWS_FOR_SUMMARY))
                    .getContent()
                    .stream()
                    .map(NewsArticleEntity::getTitle)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to load news titles for market summary: {}", e.getMessage());
            return List.of();
        }
    }
}
