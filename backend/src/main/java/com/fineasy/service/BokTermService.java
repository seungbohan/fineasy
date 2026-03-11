package com.fineasy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.config.OpenAiConfig;
import com.fineasy.dto.PageResponse;
import com.fineasy.dto.response.BokTermExplanationResponse;
import com.fineasy.dto.response.BokTermResponse;
import com.fineasy.entity.BokTermAiExplanationEntity;
import com.fineasy.entity.BokTermEntity;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.external.openai.OpenAiClient;
import com.fineasy.external.openai.OpenAiPromptBuilder;
import com.fineasy.repository.BokTermAiExplanationRepository;
import com.fineasy.repository.BokTermRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BokTermService {

    private static final Logger log = LoggerFactory.getLogger(BokTermService.class);

    private static final Duration EXPLANATION_CACHE_TTL = Duration.ofHours(24);

    private static final String EXPLANATION_CACHE_PREFIX = "bok-term:explanation:";

    private static final int EXPLANATION_MAX_TOKENS = 500;

    private final BokTermRepository bokTermRepository;
    private final BokTermAiExplanationRepository aiExplanationRepository;
    private final RedisCacheHelper redisCacheHelper;
    private final ObjectMapper objectMapper;
    private final OpenAiConfig openAiConfig;
    private final OpenAiClient openAiClient;
    private final OpenAiPromptBuilder openAiPromptBuilder;

    public BokTermService(BokTermRepository bokTermRepository,
                           BokTermAiExplanationRepository aiExplanationRepository,
                           RedisCacheHelper redisCacheHelper,
                           ObjectMapper objectMapper,
                           OpenAiConfig openAiConfig,
                           OpenAiClient openAiClient,
                           OpenAiPromptBuilder openAiPromptBuilder) {
        this.bokTermRepository = bokTermRepository;
        this.aiExplanationRepository = aiExplanationRepository;
        this.redisCacheHelper = redisCacheHelper;
        this.objectMapper = objectMapper;
        this.openAiConfig = openAiConfig;
        this.openAiClient = openAiClient;
        this.openAiPromptBuilder = openAiPromptBuilder;
    }

    public PageResponse<BokTermResponse> searchTerms(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);

        Page<BokTermEntity> result;
        if (keyword == null || keyword.isBlank()) {
            result = bokTermRepository.findAllByOrderByTermAsc(pageable);
        } else {
            result = bokTermRepository.searchByKeyword(keyword.trim(), pageable);
        }

        List<BokTermResponse> content = result.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.of(content, page, size, result.getTotalElements());
    }

    public BokTermResponse getTermById(long termId) {
        BokTermEntity entity = bokTermRepository.findById(termId)
                .orElseThrow(() -> new EntityNotFoundException("BokTerm", termId));
        return toResponse(entity);
    }

    public BokTermResponse getRandomTerm() {
        BokTermEntity entity = bokTermRepository.findRandomTerm();
        if (entity == null) {
            return null;
        }
        return toResponse(entity);
    }

    private volatile List<String> cachedTermNames;

    public List<BokTermEntity> findRelatedTerms(String newsTitle) {
        if (newsTitle == null || newsTitle.isBlank()) {
            return List.of();
        }

        List<String> termNames = getCachedTermNames();
        List<String> matchedTermNames = new ArrayList<>();

        for (String termName : termNames) {
            if (newsTitle.contains(termName)) {
                matchedTermNames.add(termName);
            }
        }

        if (matchedTermNames.isEmpty()) {
            return List.of();
        }

        if (matchedTermNames.size() > 5) {
            matchedTermNames = matchedTermNames.subList(0, 5);
        }

        return bokTermRepository.findByTermIn(matchedTermNames);
    }

    private List<String> getCachedTermNames() {
        if (cachedTermNames == null) {
            synchronized (this) {
                if (cachedTermNames == null) {
                    cachedTermNames = bokTermRepository.findAll().stream()
                            .map(BokTermEntity::getTerm)
                            .toList();
                    log.info("BOK term name cache initialized with {} entries", cachedTermNames.size());
                }
            }
        }
        return cachedTermNames;
    }

    @Transactional
    public BokTermExplanationResponse getAiExplanation(long termId) {
        BokTermEntity term = bokTermRepository.findById(termId)
                .orElseThrow(() -> new EntityNotFoundException("BokTerm", termId));

        String cacheKey = EXPLANATION_CACHE_PREFIX + termId;

        BokTermExplanationResponse cached = redisCacheHelper.getFromCache(
                cacheKey, BokTermExplanationResponse.class);
        if (cached != null) {
            log.debug("BOK term explanation cache hit (Redis) for termId: {}", termId);
            return cached;
        }

        Optional<BokTermAiExplanationEntity> dbExplanation =
                aiExplanationRepository.findByBokTerm(term);
        if (dbExplanation.isPresent()) {
            log.debug("BOK term explanation cache hit (DB) for termId: {}", termId);
            BokTermExplanationResponse fromDb = mapEntityToResponse(term, dbExplanation.get());
            redisCacheHelper.putToCache(cacheKey, fromDb, EXPLANATION_CACHE_TTL);
            return fromDb;
        }

        if (!isOpenAiAvailable()) {
            log.info("OpenAI unavailable - returning fallback explanation for termId: {}", termId);
            return buildFallbackResponse(term);
        }

        log.info("BOK term explanation cache miss - generating via AI for termId: {}", termId);
        BokTermExplanationResponse generated = generateExplanation(term);

        if (generated.keyPoints() != null && !generated.keyPoints().isEmpty()) {
            redisCacheHelper.putToCache(cacheKey, generated, EXPLANATION_CACHE_TTL);
        }

        return generated;
    }

    private boolean isOpenAiAvailable() {
        return openAiConfig.getApiKey() != null
                && !openAiConfig.getApiKey().isBlank();
    }

    private BokTermExplanationResponse generateExplanation(BokTermEntity term) {
        try {
            String systemPrompt = openAiPromptBuilder.getBokTermExplanationSystemPrompt();
            String userPrompt = openAiPromptBuilder.buildBokTermExplanationPrompt(
                    term.getTerm(), term.getEnglishTerm(), term.getDefinition());

            String aiResponse = openAiClient.chat(systemPrompt, userPrompt, EXPLANATION_MAX_TOKENS);

            return parseAndSaveExplanation(term, aiResponse);
        } catch (Exception e) {
            log.error("Failed to generate AI explanation for termId: {} - {}",
                    term.getId(), e.getMessage());
            return buildFallbackResponse(term);
        }
    }

    private BokTermExplanationResponse parseAndSaveExplanation(BokTermEntity term, String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            String simpleSummary = root.path("simpleSummary").asText("");
            String easyExplanation = root.path("easyExplanation").asText("");
            String example = root.path("example").asText("");

            List<String> keyPoints = List.of();
            JsonNode keyPointsNode = root.path("keyPoints");
            if (keyPointsNode.isArray()) {
                keyPoints = objectMapper.convertValue(keyPointsNode, new TypeReference<>() {});
            }

            String keyPointsJson = objectMapper.writeValueAsString(keyPoints);

            BokTermAiExplanationEntity entity = new BokTermAiExplanationEntity(
                    term, simpleSummary, easyExplanation, example, keyPointsJson);
            aiExplanationRepository.save(entity);

            log.info("AI explanation saved for termId: {}, term: {}", term.getId(), term.getTerm());

            return new BokTermExplanationResponse(
                    term.getId(),
                    term.getTerm(),
                    simpleSummary,
                    easyExplanation,
                    example,
                    keyPoints,
                    entity.getCreatedAt()
            );
        } catch (Exception e) {
            log.error("Failed to parse AI explanation response for termId: {} - {}",
                    term.getId(), e.getMessage());
            return buildFallbackResponse(term);
        }
    }

    private BokTermExplanationResponse mapEntityToResponse(BokTermEntity term,
                                                            BokTermAiExplanationEntity explanation) {
        List<String> keyPoints = parseKeyPoints(explanation.getKeyPoints());

        return new BokTermExplanationResponse(
                term.getId(),
                term.getTerm(),
                explanation.getSimpleSummary(),
                explanation.getEasyExplanation(),
                explanation.getExample(),
                keyPoints,
                explanation.getCreatedAt()
        );
    }

    private List<String> parseKeyPoints(String keyPointsJson) {
        if (keyPointsJson == null || keyPointsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(keyPointsJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse keyPoints JSON: {}", keyPointsJson, e);
            return List.of();
        }
    }

    private BokTermExplanationResponse buildFallbackResponse(BokTermEntity term) {
        return new BokTermExplanationResponse(
                term.getId(),
                term.getTerm(),
                term.getTerm() + "에 대한 정의입니다.",
                term.getDefinition(),
                "",
                List.of(),
                LocalDateTime.now()
        );
    }

    private BokTermResponse toResponse(BokTermEntity entity) {
        return new BokTermResponse(
                entity.getId(),
                entity.getTerm(),
                entity.getEnglishTerm(),
                entity.getDefinition(),
                entity.getCategory()
        );
    }
}
