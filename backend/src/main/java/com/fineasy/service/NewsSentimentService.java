package com.fineasy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.config.OpenAiConfig;
import com.fineasy.entity.*;
import com.fineasy.external.openai.OpenAiClient;
import com.fineasy.external.openai.OpenAiPromptBuilder;
import com.fineasy.repository.NewsArticleRepository;
import com.fineasy.repository.NewsStockTagRepository;
import com.fineasy.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class NewsSentimentService {

    private static final Logger log = LoggerFactory.getLogger(NewsSentimentService.class);

    private static final int BATCH_SIZE = 5;

    private final NewsArticleRepository newsArticleRepository;
    private final StockRepository stockRepository;
    private final NewsStockTagRepository newsStockTagRepository;
    private final OpenAiClient openAiClient;
    private final OpenAiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final OpenAiConfig openAiConfig;
    private final KeywordSentimentAnalyzer keywordSentimentAnalyzer;

    private volatile Map<String, StockEntity> stockNameCache;

    private volatile long stockNameCacheBuiltAt;

    public NewsSentimentService(NewsArticleRepository newsArticleRepository,
                                 StockRepository stockRepository,
                                 NewsStockTagRepository newsStockTagRepository,
                                 OpenAiClient openAiClient,
                                 OpenAiPromptBuilder promptBuilder,
                                 ObjectMapper objectMapper,
                                 OpenAiConfig openAiConfig,
                                 KeywordSentimentAnalyzer keywordSentimentAnalyzer) {
        this.newsArticleRepository = newsArticleRepository;
        this.stockRepository = stockRepository;
        this.newsStockTagRepository = newsStockTagRepository;
        this.openAiClient = openAiClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.openAiConfig = openAiConfig;
        this.keywordSentimentAnalyzer = keywordSentimentAnalyzer;
    }

    @Transactional
    public void analyzeSentiment(List<NewsArticleEntity> articles) {
        if (articles.isEmpty()) {
            return;
        }

        log.info("Starting sentiment analysis for {} articles", articles.size());

        Set<NewsArticleEntity> nonStockRelated = new HashSet<>();

        boolean aiAvailable = isOpenAiAvailable();

        if (!aiAvailable) {
            log.info("OpenAI not configured, using keyword-based analysis + name matching");
            for (NewsArticleEntity article : articles) {
                analyzeWithKeywords(article);
            }

            tagStocksByNameMatch(articles);
        } else {

            for (int i = 0; i < articles.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, articles.size());
                List<NewsArticleEntity> batch = articles.subList(i, end);

                try {
                    Set<NewsArticleEntity> batchNonRelated = analyzeBatchWithAi(batch);
                    nonStockRelated.addAll(batchNonRelated);
                } catch (Exception e) {
                    log.error("OpenAI batch failed ({}-{}): {}, falling back to keywords",
                            i, end, e.getMessage());
                    for (NewsArticleEntity article : batch) {
                        analyzeWithKeywords(article);
                    }
                    tagStocksByNameMatch(batch);
                }
            }
        }

        List<NewsArticleEntity> toKeep = new ArrayList<>();
        List<NewsArticleEntity> toDelete = new ArrayList<>();

        for (NewsArticleEntity article : articles) {
            if (nonStockRelated.contains(article)) {
                toDelete.add(article);
            } else {

                toKeep.add(article);
            }
        }

        if (!toKeep.isEmpty()) {
            try {
                newsArticleRepository.saveAll(toKeep);
            } catch (Exception e) {
                log.error("Failed to save analyzed articles, saving individually", e);
                for (NewsArticleEntity article : toKeep) {
                    try {
                        newsArticleRepository.save(article);
                    } catch (Exception ex) {
                        log.error("Failed to save article id={}: {}", article.getId(), ex.getMessage());
                    }
                }
            }
        }

        if (!toDelete.isEmpty()) {
            try {
                newsArticleRepository.deleteAll(toDelete);
                log.info("Deleted {} non-stock-related articles", toDelete.size());
            } catch (Exception e) {
                log.error("Failed to bulk delete articles, deleting individually", e);
                for (NewsArticleEntity article : toDelete) {
                    try {
                        newsArticleRepository.deleteById(article.getId());
                    } catch (Exception ex) {
                        log.error("Failed to delete article id={}: {}", article.getId(), ex.getMessage());
                    }
                }
            }
        }

        log.info("Kept {} stock-related articles, deleted {} irrelevant",
                toKeep.size(), toDelete.size());
    }

    private Set<NewsArticleEntity> analyzeBatchWithAi(List<NewsArticleEntity> batch) {
        Set<NewsArticleEntity> nonStockRelated = new HashSet<>();
        Map<String, StockEntity> cache = getStockNameCache();

        List<String> titles = batch.stream()
                .map(NewsArticleEntity::getTitle)
                .toList();

        String systemPrompt = promptBuilder.getSentimentSystemPrompt();
        String userPrompt = promptBuilder.buildSentimentPrompt(titles);

        String response = openAiClient.chat(systemPrompt, userPrompt);

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("results");

            if (!results.isArray()) {
                log.warn("Unexpected AI response format: {}", response);
                batch.forEach(this::setDefaultSentiment);
                tagStocksByNameMatch(batch);
                return nonStockRelated;
            }

            for (JsonNode result : results) {
                int index = result.path("index").asInt(0) - 1;
                if (index < 0 || index >= batch.size()) continue;

                NewsArticleEntity article = batch.get(index);

                boolean stockRelated = result.path("stockRelated").asBoolean(true);
                if (!stockRelated) {
                    nonStockRelated.add(article);
                    continue;
                }

                String titleKo = result.path("titleKo").asText(null);
                if (titleKo != null && !titleKo.isBlank() && !"null".equals(titleKo)) {
                    article.updateTitle(titleKo);
                }

                String sentimentStr = result.path("sentiment").asText("NEUTRAL").toUpperCase();
                double score = result.path("score").asDouble(0.5);
                try {
                    updateSentiment(article, Sentiment.valueOf(sentimentStr), score);
                } catch (IllegalArgumentException e) {
                    setDefaultSentiment(article);
                }

                // Process enhanced stock impacts (new format with impact metadata)
                JsonNode stockImpacts = result.path("stockImpacts");
                if (stockImpacts.isArray() && !stockImpacts.isEmpty()) {
                    processStockImpacts(article, stockImpacts, cache);
                } else {
                    // Fallback: try legacy "stocks" array for backward compatibility
                    JsonNode stocksNode = result.path("stocks");
                    if (stocksNode.isArray()) {
                        for (JsonNode stockName : stocksNode) {
                            String name = stockName.asText().trim();
                            StockEntity entity = cache.get(name);
                            if (entity != null && !article.getTaggedStocks().contains(entity)) {
                                article.getTaggedStocks().add(entity);
                                saveEnhancedTag(article, entity, ImpactType.DIRECT, ImpactDirection.NEUTRAL, 0.5);
                            }
                        }
                    }
                }

                tagSingleArticleByNameMatch(article, cache);
            }

            for (NewsArticleEntity article : batch) {
                if (article.getSentiment() == null && !nonStockRelated.contains(article)) {
                    setDefaultSentiment(article);
                    tagSingleArticleByNameMatch(article, cache);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", response, e);
            batch.forEach(this::setDefaultSentiment);
            tagStocksByNameMatch(batch);
        }

        return nonStockRelated;
    }

    private void processStockImpacts(NewsArticleEntity article, JsonNode stockImpacts,
                                      Map<String, StockEntity> cache) {
        for (JsonNode impact : stockImpacts) {
            String name = impact.path("name").asText("").trim();
            if (name.isEmpty()) continue;

            StockEntity entity = cache.get(name);
            if (entity == null) continue;

            // Add to legacy taggedStocks for backward compatibility
            if (!article.getTaggedStocks().contains(entity)) {
                article.getTaggedStocks().add(entity);
            }

            // Parse impact metadata
            ImpactType impactType = parseImpactType(impact.path("impact").asText("DIRECT"));
            ImpactDirection direction = parseImpactDirection(impact.path("direction").asText("NEUTRAL"));
            double relevance = impact.path("relevance").asDouble(0.5);

            saveEnhancedTag(article, entity, impactType, direction, relevance);
        }
    }

    private void saveEnhancedTag(NewsArticleEntity article, StockEntity stock,
                                  ImpactType impactType, ImpactDirection direction,
                                  double relevance) {
        try {
            if (article.getId() == null) return; // Article not yet persisted

            Optional<NewsStockTagEntity> existing =
                    newsStockTagRepository.findByNewsArticleIdAndStockId(article.getId(), stock.getId());

            if (existing.isPresent()) {
                existing.get().updateImpact(impactType, direction, relevance);
                newsStockTagRepository.save(existing.get());
            } else {
                newsStockTagRepository.save(
                        new NewsStockTagEntity(article, stock, impactType, direction, relevance));
            }
        } catch (Exception e) {
            log.debug("Failed to save enhanced tag for article={}, stock={}: {}",
                    article.getId(), stock.getStockCode(), e.getMessage());
        }
    }

    private ImpactType parseImpactType(String value) {
        try {
            return ImpactType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ImpactType.DIRECT;
        }
    }

    private ImpactDirection parseImpactDirection(String value) {
        try {
            return ImpactDirection.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ImpactDirection.NEUTRAL;
        }
    }

    private void tagStocksByNameMatch(List<NewsArticleEntity> articles) {
        Map<String, StockEntity> cache = getStockNameCache();
        for (NewsArticleEntity article : articles) {
            tagSingleArticleByNameMatch(article, cache);
        }
    }

    private void tagSingleArticleByNameMatch(NewsArticleEntity article,
                                               Map<String, StockEntity> cache) {
        String title = article.getTitle();
        if (title == null) return;

        for (Map.Entry<String, StockEntity> entry : cache.entrySet()) {
            if (title.contains(entry.getKey())) {
                if (!article.getTaggedStocks().contains(entry.getValue())) {
                    article.getTaggedStocks().add(entry.getValue());
                    // Name match defaults to DIRECT/NEUTRAL
                    saveEnhancedTag(article, entry.getValue(),
                            ImpactType.DIRECT, ImpactDirection.NEUTRAL, 0.5);
                }
            }
        }
    }

    private static final long CACHE_TTL_MS = 60 * 60 * 1000L;

    private Map<String, StockEntity> getStockNameCache() {
        boolean expired = stockNameCache != null
                && (System.currentTimeMillis() - stockNameCacheBuiltAt) > CACHE_TTL_MS;
        if (stockNameCache == null || expired) {
            synchronized (this) {

                boolean expiredInLock = stockNameCache != null
                        && (System.currentTimeMillis() - stockNameCacheBuiltAt) > CACHE_TTL_MS;
                if (stockNameCache == null || expiredInLock) {
                    Map<String, StockEntity> cache = new HashMap<>();
                    List<StockEntity> allStocks = stockRepository.findAll();
                    for (StockEntity stock : allStocks) {
                        cache.put(stock.getStockName(), stock);
                        cache.put(stock.getStockCode(), stock);
                    }
                    stockNameCache = cache;
                    stockNameCacheBuiltAt = System.currentTimeMillis();
                    log.info("Stock name cache initialized/refreshed with {} entries", cache.size());
                }
            }
        }
        return stockNameCache;
    }

    private void updateSentiment(NewsArticleEntity article, Sentiment sentiment, double score) {
        article.updateSentiment(sentiment, score);
    }

    private void setDefaultSentiment(NewsArticleEntity article) {
        updateSentiment(article, Sentiment.NEUTRAL, 0.5);
    }

    private void analyzeWithKeywords(NewsArticleEntity article) {
        KeywordSentimentAnalyzer.SentimentResult result =
                keywordSentimentAnalyzer.analyze(article.getTitle());
        updateSentiment(article, result.sentiment(), result.score());
    }

    private boolean isOpenAiAvailable() {
        String apiKey = openAiConfig.getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }
}
