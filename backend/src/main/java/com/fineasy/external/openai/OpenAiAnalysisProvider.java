package com.fineasy.external.openai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.dto.response.AnalysisReportResponse;
import com.fineasy.dto.response.PredictionResponse;
import com.fineasy.dto.response.StockFinancialsResponse;
import com.fineasy.entity.*;
import com.fineasy.repository.NewsStockTagRepository;
import com.fineasy.service.AiAnalysisProvider;
import com.fineasy.service.EmbeddingService;
import com.fineasy.service.FinanceOntology;
import com.fineasy.service.GlobalEventService;
import com.fineasy.service.MacroService;
import com.fineasy.service.NewsService;
import com.fineasy.service.StockDataProvider;
import com.fineasy.service.StockRelationInferenceService;
import com.fineasy.service.StockService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@Primary
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiAnalysisProvider implements AiAnalysisProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAnalysisProvider.class);

    private static final String DISCLAIMER =
            "이 분석은 AI가 생성한 참고 자료이며 투자 권유가 아닙니다. " +
            "투자 결정은 본인의 판단과 책임 하에 이루어져야 합니다.";

    private static final int MAX_TOKENS_REPORT = 800;
    private static final int MAX_TOKENS_PREDICTION = 400;

    private static final int MAX_REASONING_TOKENS_REPORT = 8000;
    private static final int MAX_REASONING_TOKENS_PREDICTION = 4000;

    private final OpenAiClient openAiClient;
    private final OpenAiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final StockService stockService;
    private final NewsService newsService;
    private final MacroService macroService;
    private final StockDataProvider stockDataProvider;
    private final GlobalEventService globalEventService;
    private final EmbeddingService embeddingService;
    private final FinanceOntology financeOntology;
    private final NewsStockTagRepository newsStockTagRepository;
    private final StockRelationInferenceService stockRelationInferenceService;

    public OpenAiAnalysisProvider(OpenAiClient openAiClient,
                                   OpenAiPromptBuilder promptBuilder,
                                   ObjectMapper objectMapper,
                                   StockService stockService,
                                   NewsService newsService,
                                   MacroService macroService,
                                   StockDataProvider stockDataProvider,
                                   GlobalEventService globalEventService,
                                   EmbeddingService embeddingService,
                                   FinanceOntology financeOntology,
                                   NewsStockTagRepository newsStockTagRepository,
                                   StockRelationInferenceService stockRelationInferenceService) {
        this.openAiClient = openAiClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.stockService = stockService;
        this.newsService = newsService;
        this.macroService = macroService;
        this.stockDataProvider = stockDataProvider;
        this.globalEventService = globalEventService;
        this.embeddingService = embeddingService;
        this.financeOntology = financeOntology;
        this.newsStockTagRepository = newsStockTagRepository;
        this.stockRelationInferenceService = stockRelationInferenceService;
    }

    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "generateReportFallback")
    @Retry(name = "openai")
    public AnalysisReportResponse generateReport(String stockCode) {
        log.info("Generating RAG+Ontology enhanced AI analysis report for stock: {}", stockCode);

        StockEntity stock = stockService.getStockEntityByCode(stockCode);
        StockFinancialsResponse financials = fetchFinancialsSafely(stockCode);
        List<MacroIndicatorEntity> macroIndicators = macroService.getLatestIndicatorEntities();
        List<String> globalEventSummaries = fetchGlobalEventSummaries();

        // RAG: semantic search for relevant news instead of simple keyword/tag matching
        List<String> recentNewsTitles = fetchSemanticNews(stock.getStockName(), stockCode, 15);

        // Ontology: inject sector domain knowledge
        String ontologyContext = financeOntology.buildOntologyContext(stockCode, stock.getSector());

        String userPrompt = promptBuilder.buildReportPrompt(
                stock.getStockName(), stockCode, macroIndicators,
                recentNewsTitles, globalEventSummaries, financials);

        // Append ontology context to user prompt
        if (!ontologyContext.isEmpty()) {
            userPrompt = userPrompt + "\n" + ontologyContext;
        }

        String systemPrompt = promptBuilder.getReportSystemPrompt();

        String aiResponse = openAiClient.chatReasoning(systemPrompt, userPrompt, MAX_REASONING_TOKENS_REPORT);
        return parseReportResponse(stockCode, aiResponse);
    }

    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "generatePredictionFallback")
    @Retry(name = "openai")
    public PredictionResponse generatePrediction(String stockCode, String period) {
        log.info("Generating RAG+Ontology enhanced AI prediction for stock: {}, period: {}", stockCode, period);

        StockEntity stock = stockService.getStockEntityByCode(stockCode);
        Double sentimentAvg = newsService.getAverageSentimentScore(stockCode, 20);
        List<MacroIndicatorEntity> macroIndicators = macroService.getLatestIndicatorEntities();
        StockFinancialsResponse financials = fetchFinancialsSafely(stockCode);

        // RAG: semantic search for relevant news
        List<String> recentNewsTitles = fetchSemanticNews(stock.getStockName(), stockCode, 15);

        // Ontology context
        String ontologyContext = financeOntology.buildOntologyContext(stockCode, stock.getSector());

        String userPrompt = promptBuilder.buildPredictionPrompt(
                stock.getStockName(), stockCode, period, sentimentAvg, macroIndicators, financials, recentNewsTitles);

        if (!ontologyContext.isEmpty()) {
            userPrompt = userPrompt + "\n" + ontologyContext;
        }

        String systemPrompt = promptBuilder.getPredictionSystemPrompt();

        String aiResponse = openAiClient.chatReasoning(systemPrompt, userPrompt, MAX_REASONING_TOKENS_PREDICTION);
        return parsePredictionResponse(stockCode, period, aiResponse);
    }

    private List<String> fetchGlobalEventSummaries() {
        try {
            var response = globalEventService.getHighRiskEvents();
            if (response.events() == null || response.events().isEmpty()) {

                var allEvents = globalEventService.getAllEvents(0, 5);
                return allEvents.events().stream()
                        .map(e -> String.format("[%s] %s - %s", e.eventType(), e.title(), e.summary()))
                        .toList();
            }
            return response.events().stream()
                    .limit(5)
                    .map(e -> String.format("[%s/%s] %s - %s", e.eventType(), e.riskLevel(), e.title(), e.summary()))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch global events for report: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Graph-enhanced RAG news retrieval:
     * 1. Direct tagged news with impact metadata (from NewsStockTagEntity)
     * 2. Related stocks' news via relationship graph (multi-hop)
     * 3. Semantic search to fill remaining slots
     * Falls back to keyword-based search if all else fails.
     */
    private List<String> fetchSemanticNews(String stockName, String stockCode, int limit) {
        List<String> result = new ArrayList<>();
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        try {
            // Step 1: Direct tagged news with impact context
            var directTags = newsStockTagRepository.findByStockCodeAndImpactType(
                    stockCode, com.fineasy.entity.ImpactType.DIRECT, since,
                    org.springframework.data.domain.PageRequest.of(0, 8));

            for (var tag : directTags) {
                String prefix = tag.getImpactDirection() != null
                        ? "[" + tag.getImpactDirection() + "] "
                        : "";
                result.add(prefix + tag.getNewsArticle().getTitle());
            }

            // Step 2: Supply chain / competitor news from relationship graph
            if (result.size() < limit) {
                var relatedTags = newsStockTagRepository.findByStockCodeAndImpactTypeIn(
                        stockCode,
                        List.of(com.fineasy.entity.ImpactType.SUPPLY_CHAIN,
                                com.fineasy.entity.ImpactType.COMPETITOR,
                                com.fineasy.entity.ImpactType.INDIRECT),
                        since,
                        org.springframework.data.domain.PageRequest.of(0, 4));

                for (var tag : relatedTags) {
                    String prefix = "[" + tag.getImpactType() + "] ";
                    result.add(prefix + tag.getNewsArticle().getTitle());
                }
            }

            // Step 3: News from graph-related stocks (multi-hop)
            if (result.size() < limit) {
                try {
                    StockEntity stock = stockService.getStockEntityByCode(stockCode);
                    List<Long> relatedIds = stockRelationInferenceService.findRelatedStockIds(
                            stock.getId(), 2, 0.5);

                    if (!relatedIds.isEmpty()) {
                        List<String> relatedCodes = relatedIds.stream()
                                .map(id -> {
                                    try {
                                        return stockService.getStockEntityById(id).getStockCode();
                                    } catch (Exception e) {
                                        return null;
                                    }
                                })
                                .filter(java.util.Objects::nonNull)
                                .toList();

                        if (!relatedCodes.isEmpty()) {
                            var relatedNews = newsService.getRecentNewsByStockCodes(
                                    relatedCodes, limit - result.size());
                            relatedNews.forEach(title -> result.add("[관련종목] " + title));
                        }
                    }
                } catch (Exception e) {
                    log.debug("Graph-based news search failed for {}: {}", stockCode, e.getMessage());
                }
            }

            // Step 4: Fill remaining with semantic search
            if (result.size() < limit) {
                String query = stockName + " " + stockCode;
                var semanticResults = embeddingService.searchSimilarNews(
                        query, limit - result.size(), since);

                if (semanticResults != null) {
                    Set<String> existingTitles = new HashSet<>(result);
                    for (var article : semanticResults) {
                        String title = article.getTitle();
                        if (!existingTitles.contains(title)
                                && !existingTitles.contains("[POSITIVE] " + title)
                                && !existingTitles.contains("[NEGATIVE] " + title)
                                && !existingTitles.contains("[NEUTRAL] " + title)) {
                            result.add(title);
                        }
                    }
                }
            }

            if (result.size() >= 5) {
                log.debug("Graph+RAG: found {} articles for {} (direct={}, total={})",
                        result.size(), stockCode, directTags.size(), result.size());
                return result.stream().limit(limit).toList();
            }
        } catch (Exception e) {
            log.debug("Graph+RAG search failed for {}, falling back to keyword: {}", stockCode, e.getMessage());
        }

        // Fallback to existing keyword-based search
        return newsService.getRecentNewsTitles(stockCode, limit);
    }

    private StockFinancialsResponse fetchFinancialsSafely(String stockCode) {
        try {
            return stockDataProvider.getFinancials(stockCode);
        } catch (Exception e) {
            log.warn("Failed to fetch financials for stock {}: {}", stockCode, e.getMessage());
            return null;
        }
    }

    private AnalysisReportResponse parseReportResponse(String stockCode, String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            String summary = root.path("summary").asText("분석 데이터를 확인하세요.");
            String description = root.path("description").asText("");

            List<String> keyPoints = List.of();
            if (root.has("keyPoints") && root.get("keyPoints").isArray()) {
                keyPoints = objectMapper.convertValue(root.get("keyPoints"),
                        new TypeReference<List<String>>() {});
            }

            String investmentOpinion = root.path("investmentOpinion").asText("NEUTRAL").toUpperCase();
            if (!List.of("POSITIVE", "NEGATIVE", "NEUTRAL").contains(investmentOpinion)) {

                investmentOpinion = switch (investmentOpinion) {
                    case "BUY" -> "POSITIVE";
                    case "SELL" -> "NEGATIVE";
                    default -> "NEUTRAL";
                };
            }

            Map<String, Object> analysisData = new java.util.LinkedHashMap<>();
            if (root.has("macroImpact")) {
                analysisData.put("macroImpact", root.path("macroImpact").asText(""));
            }
            if (root.has("newsAnalysis")) {
                analysisData.put("newsAnalysis", root.path("newsAnalysis").asText(""));
            }
            if (root.has("globalEventImpact")) {
                analysisData.put("globalEventImpact", root.path("globalEventImpact").asText(""));
            }
            if (root.has("sentimentReason")) {
                analysisData.put("sentimentReason", root.path("sentimentReason").asText(""));
            }

            return new AnalysisReportResponse(
                    stockCode, LocalDateTime.now(), summary, description,
                    keyPoints, investmentOpinion, DISCLAIMER, analysisData
            );
        } catch (Exception e) {
            log.error("Failed to parse AI report response for stock {}: {}", stockCode, aiResponse, e);
            return createDefaultReport(stockCode);
        }
    }

    private PredictionResponse parsePredictionResponse(String stockCode, String period, String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            String directionStr = root.path("direction").asText("SIDEWAYS").toUpperCase();
            PredictionDirection direction = PredictionDirection.valueOf(directionStr);

            int confidence = root.path("confidence").asInt(50);
            confidence = Math.max(0, Math.min(100, confidence));

            String valuation = root.path("valuation").asText("FAIR").toUpperCase();
            if (!List.of("UNDERVALUED", "FAIR", "OVERVALUED").contains(valuation)) {
                valuation = "FAIR";
            }

            List<String> reasons = List.of();
            if (root.has("reasons") && root.get("reasons").isArray()) {
                reasons = objectMapper.convertValue(root.get("reasons"),
                        new TypeReference<List<String>>() {});
            }

            return new PredictionResponse(
                    stockCode, period, direction, confidence,
                    reasons, DISCLAIMER, LocalDateTime.now(), valuation
            );
        } catch (Exception e) {
            log.error("Failed to parse AI prediction response for stock {}: {}", stockCode, aiResponse, e);
            return createDefaultPrediction(stockCode, period);
        }
    }

    @SuppressWarnings("unused")
    private AnalysisReportResponse generateReportFallback(String stockCode, Throwable t) {
        log.warn("Report generation fallback triggered for stock {}: {}", stockCode, t.getMessage());
        return createDefaultReport(stockCode);
    }

    @SuppressWarnings("unused")
    private PredictionResponse generatePredictionFallback(String stockCode, String period, Throwable t) {
        log.warn("Prediction generation fallback triggered for stock {}: {}", stockCode, t.getMessage());
        return createDefaultPrediction(stockCode, period);
    }

    private AnalysisReportResponse createDefaultReport(String stockCode) {
        return new AnalysisReportResponse(
                stockCode,
                LocalDateTime.now(),
                "AI 분석 서비스를 일시적으로 사용할 수 없습니다.",
                "현재 AI 분석 서비스에 일시적 문제가 발생하여 실시간 분석을 제공할 수 없습니다. " +
                        "잠시 후 다시 시도해 주세요.",
                List.of("AI 서비스 일시 장애로 분석 생성 불가",
                        "뉴스 기반 분석은 잠시 후 다시 시도해 주세요",
                        "거시경제 지표는 별도 메뉴에서 확인 가능"),
                "NEUTRAL",
                DISCLAIMER,
                Map.of()
        );
    }

    private PredictionResponse createDefaultPrediction(String stockCode, String period) {
        return new PredictionResponse(
                stockCode,
                period,
                PredictionDirection.SIDEWAYS,
                0,
                List.of("AI 분석 서비스를 일시적으로 사용할 수 없습니다.",
                        "잠시 후 다시 시도해 주세요.",
                        "기업 펀더멘털 데이터를 참고하여 직접 판단해 주세요."),
                DISCLAIMER,
                LocalDateTime.now(),
                "FAIR"
        );
    }
}
