package com.fineasy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.entity.EventType;
import com.fineasy.entity.GlobalEventEntity;
import com.fineasy.entity.NewsArticleEntity;
import com.fineasy.entity.RiskLevel;
import com.fineasy.external.openai.OpenAiClient;
import com.fineasy.repository.GlobalEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GlobalEventClassifierService {

    private static final Logger log = LoggerFactory.getLogger(GlobalEventClassifierService.class);

    private static final int MAX_TOKENS = 200;

    private static final Map<EventType, List<String>> KEYWORD_MAP = Map.of(
            EventType.GEOPOLITICAL, List.of(

                    "war", "conflict", "sanctions", "tariff", "trade war",
                    "missile", "invasion", "nato", "opec",
                    "taiwan", "north korea", "iran", "russia", "ukraine",
                    "embargo", "escalation", "ceasefire", "nuclear",

                    "전쟁", "분쟁", "제재", "관세", "무역전쟁",
                    "미사일", "침공", "갈등", "긴장", "대만",
                    "북한", "우크라이나", "중동", "이란",
                    "나토", "오펙", "보복", "군사"
            ),
            EventType.FISCAL, List.of(

                    "fomc", "fed rate", "rate hike", "rate cut", "interest rate",
                    "federal reserve", "powell", "jackson hole",
                    "ecb", "boj", "central bank", "monetary policy",
                    "quantitative easing", "quantitative tightening",

                    "cpi", "ppi", "pce", "nonfarm", "payroll", "unemployment",
                    "gdp", "ism", "retail sales", "consumer confidence",
                    "inflation", "deflation", "recession",

                    "debt ceiling", "shutdown", "election", "regulation", "tax",
                    "stimulus", "budget",

                    "금리", "기준금리", "인상", "인하", "연준", "파월",
                    "통화정책", "양적완화", "양적긴축",
                    "소비자물가", "생산자물가", "고용", "실업",
                    "인플레이션", "디플레이션", "경기침체",

                    "부채한도", "셧다운", "대선", "총선", "규제", "세제",
                    "부양책", "예산"
            ),
            EventType.BLACK_SWAN, List.of(

                    "pandemic", "crash", "collapse", "bankruptcy", "crisis", "default",
                    "bank run", "bank failure", "contagion", "systemic risk",
                    "black swan", "circuit breaker", "meltdown",
                    "earthquake", "hurricane", "flood",

                    "팬데믹", "폭락", "붕괴", "파산", "위기", "디폴트",
                    "뱅크런", "전염", "시스템 리스크", "블랙스완",
                    "서킷브레이커", "급락", "대폭락", "금융위기"
            ),
            EventType.INDUSTRY, List.of(

                    "earnings", "M&A", "acquisition", "IPO", "buyback",
                    "dividend", "semiconductor", "guidance",
                    "profit", "revenue", "layoff", "restructuring",
                    "upgrade", "downgrade", "outlook",
                    "sec", "etf", "bitcoin", "crypto", "stablecoin",
                    "halving", "whale", "defi",
                    "vix", "yield curve", "treasury", "bond",
                    "oil price", "gold price", "dollar index",

                    "실적", "인수합병", "상장", "자사주", "배당", "반도체",
                    "매출", "영업이익", "구조조정", "감원",
                    "비트코인", "이더리움", "암호화폐", "가상자산",
                    "공포지수", "국채", "수익률", "유가", "금값", "환율",
                    "외국인", "기관", "공매도"
            )
    );

    private static final String SYSTEM_PROMPT =
            "You are a financial news classifier. Classify the given news headline into one of the following event types and risk levels. " +
            "Respond ONLY with a JSON object.\n\n" +
            "Event types:\n" +
            "- GEOPOLITICAL: Geopolitical risks (war, sanctions, trade conflicts, territorial disputes)\n" +
            "- FISCAL: Fiscal/political issues (debt ceiling, government shutdown, elections, regulations, tax policy)\n" +
            "- INDUSTRY: Industry/corporate news (M&A, IPO, earnings, dividends, sector trends)\n" +
            "- BLACK_SWAN: Black swan/sudden events (pandemic, market crash, collapse, bankruptcy, systemic crisis)\n\n" +
            "Risk levels:\n" +
            "- LOW: Minimal market impact\n" +
            "- MEDIUM: Some sector impact possible\n" +
            "- HIGH: Significant market-wide impact\n" +
            "- CRITICAL: Immediate and widespread market impact\n\n" +
            "Response format: {\"eventType\": \"...\", \"riskLevel\": \"...\", \"summary\": \"...(2-3 sentence summary in Korean)\"}";

    private final GlobalEventRepository globalEventRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private OpenAiClient openAiClient;

    public GlobalEventClassifierService(GlobalEventRepository globalEventRepository,
                                         ObjectMapper objectMapper) {
        this.globalEventRepository = globalEventRepository;
        this.objectMapper = objectMapper;
    }

    public List<KeywordMatchResult> filterByKeywords(List<NewsArticleEntity> articles) {
        List<KeywordMatchResult> results = new ArrayList<>();

        List<String> articleUrls = articles.stream()
                .map(NewsArticleEntity::getOriginalUrl)
                .filter(java.util.Objects::nonNull)
                .toList();
        Set<String> existingUrls = articleUrls.isEmpty()
                ? Set.of()
                : globalEventRepository.findExistingSourceUrls(articleUrls);

        for (NewsArticleEntity article : articles) {

            if (existingUrls.contains(article.getOriginalUrl())) {
                continue;
            }

            String titleLower = article.getTitle().toLowerCase();
            EventType matchedType = matchKeyword(titleLower);

            if (matchedType != null) {
                results.add(new KeywordMatchResult(article, matchedType));
            }
        }

        log.info("Keyword filter: {}/{} articles matched", results.size(), articles.size());
        return results;
    }

    public List<GlobalEventEntity> classifyAndSave(List<KeywordMatchResult> matchResults) {
        List<GlobalEventEntity> savedEvents = new ArrayList<>();

        for (KeywordMatchResult match : matchResults) {
            try {
                GlobalEventEntity event = classifySingle(match);
                if (event != null) {
                    savedEvents.add(globalEventRepository.save(event));
                }
            } catch (Exception e) {
                log.error("Failed to classify article: {}", match.article().getTitle(), e);
            }
        }

        log.info("Classified and saved {} global events", savedEvents.size());
        return savedEvents;
    }

    private GlobalEventEntity classifySingle(KeywordMatchResult match) {
        NewsArticleEntity article = match.article();

        if (globalEventRepository.existsBySourceUrl(article.getOriginalUrl())) {
            return null;
        }

        EventType eventType = match.keywordMatchedType();
        RiskLevel riskLevel = determineDefaultRiskLevel(eventType);
        String summary = article.getTitle();

        if (openAiClient != null) {
            try {
                ClassificationResult aiResult = classifyWithOpenAi(article.getTitle());
                if (aiResult != null) {
                    eventType = aiResult.eventType();
                    riskLevel = aiResult.riskLevel();
                    summary = aiResult.summary();
                }
            } catch (Exception e) {
                log.warn("OpenAI classification failed for '{}', using keyword-based fallback: {}",
                        article.getTitle(), e.getMessage());
            }
        }

        return new GlobalEventEntity(
                null,
                eventType,
                article.getTitle(),
                summary,
                article.getOriginalUrl(),
                article.getSourceName(),
                riskLevel,
                article.getPublishedAt(),
                null
        );
    }

    private ClassificationResult classifyWithOpenAi(String title) {
        String userPrompt = "Classify this news headline:\n\"" + title + "\"";
        String response = openAiClient.chat(SYSTEM_PROMPT, userPrompt, MAX_TOKENS);

        try {
            JsonNode root = objectMapper.readTree(response);

            String eventTypeStr = root.path("eventType").asText("");
            String riskLevelStr = root.path("riskLevel").asText("");
            String summary = root.path("summary").asText(title);

            EventType eventType = parseEventType(eventTypeStr);
            RiskLevel riskLevel = parseRiskLevel(riskLevelStr);

            if (eventType == null || riskLevel == null) {
                log.warn("Invalid OpenAI classification response: eventType={}, riskLevel={}",
                        eventTypeStr, riskLevelStr);
                return null;
            }

            return new ClassificationResult(eventType, riskLevel, summary);
        } catch (Exception e) {
            log.error("Failed to parse OpenAI classification response: {}", response, e);
            return null;
        }
    }

    private EventType matchKeyword(String titleLower) {
        EventType[] priority = {
                EventType.BLACK_SWAN,
                EventType.GEOPOLITICAL,
                EventType.FISCAL,
                EventType.INDUSTRY
        };

        for (EventType type : priority) {
            List<String> keywords = KEYWORD_MAP.get(type);
            if (keywords != null) {
                for (String keyword : keywords) {
                    if (titleLower.contains(keyword.toLowerCase())) {
                        return type;
                    }
                }
            }
        }
        return null;
    }

    private RiskLevel determineDefaultRiskLevel(EventType eventType) {
        return switch (eventType) {
            case BLACK_SWAN -> RiskLevel.CRITICAL;
            case GEOPOLITICAL -> RiskLevel.HIGH;
            case FISCAL -> RiskLevel.MEDIUM;
            case INDUSTRY -> RiskLevel.LOW;
        };
    }

    private EventType parseEventType(String value) {
        try {
            return EventType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private RiskLevel parseRiskLevel(String value) {
        try {
            return RiskLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public record KeywordMatchResult(
            NewsArticleEntity article,
            EventType keywordMatchedType
    ) {}

    private record ClassificationResult(
            EventType eventType,
            RiskLevel riskLevel,
            String summary
    ) {}
}
