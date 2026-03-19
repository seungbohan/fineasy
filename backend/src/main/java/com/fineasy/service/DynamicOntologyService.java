package com.fineasy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.entity.DynamicOntologyEntity;
import com.fineasy.entity.NewsArticleEntity;
import com.fineasy.entity.StockEntity;
import com.fineasy.external.openai.OpenAiClient;
import com.fineasy.repository.DynamicOntologyRepository;
import com.fineasy.repository.NewsArticleRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Periodically extracts dynamic market knowledge from recent news using LLM.
 * Produces hot issues, stock relations, and emerging themes per sector.
 * Runs weekly (Sunday 4 AM) with minimal LLM cost (~$0.10/run via gpt-4o-mini).
 */
@Service
public class DynamicOntologyService {

    private static final Logger log = LoggerFactory.getLogger(DynamicOntologyService.class);

    private static final int MAX_NEWS_PER_SECTOR = 30;
    private static final int VALIDITY_DAYS = 14;

    private static final String SYSTEM_PROMPT = """
            당신은 금융 시장 온톨로지 전문가입니다. 특정 섹터의 최근 뉴스 헤드라인을 분석하여
            구조화된 시장 지식을 추출합니다. 뉴스에서 발견되는 패턴, 관계, 트렌드를 정리하세요.

            응답은 반드시 아래 JSON 형식으로 제공하세요:
            {
              "hotIssues": [
                {
                  "issue": "이슈 요약 (20자 이내)",
                  "impact": "영향 설명 (50자 이내)",
                  "relatedStocks": ["종목명1", "종목명2"]
                }
              ],
              "stockRelations": [
                {
                  "from": "종목명 또는 티커",
                  "to": "종목명 또는 티커",
                  "type": "SUPPLY_CHAIN|COMPETITOR|CUSTOMER",
                  "description": "관계 설명 (30자 이내)"
                }
              ],
              "emergingThemes": [
                {
                  "theme": "테마명 (15자 이내)",
                  "description": "설명 (30자 이내)",
                  "stocks": ["종목명1", "종목명2"]
                }
              ]
            }

            규칙:
            - hotIssues: 최대 3개, 현재 가장 중요한 이슈만
            - stockRelations: 최대 5개, 뉴스에서 명확히 드러나는 관계만
            - emergingThemes: 최대 2개, 새로 부상하는 테마만 (기존 메인 테마 제외)
            - 추측하지 말고 뉴스에서 직접 확인되는 내용만 추출
            """;

    private final DynamicOntologyRepository dynamicOntologyRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private OpenAiClient openAiClient;

    public DynamicOntologyService(DynamicOntologyRepository dynamicOntologyRepository,
                                   NewsArticleRepository newsArticleRepository,
                                   ObjectMapper objectMapper) {
        this.dynamicOntologyRepository = dynamicOntologyRepository;
        this.newsArticleRepository = newsArticleRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "0 0 4 * * SUN")
    @SchedulerLock(name = "refreshDynamicOntology", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    @Transactional
    public void refreshOntology() {
        if (openAiClient == null) {
            log.info("OpenAI not available, skipping dynamic ontology refresh.");
            return;
        }

        log.info("Starting dynamic ontology refresh...");

        // Get recent tagged news grouped by sector
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<NewsArticleEntity> recentNews = newsArticleRepository
                .findBySentimentIsNull(PageRequest.of(0, 0)).isEmpty()
                ? List.of()
                : List.of();

        // Use tagged news that has stocks assigned
        List<NewsArticleEntity> taggedNews = fetchRecentTaggedNews(since);

        if (taggedNews.isEmpty()) {
            log.info("No recent tagged news found, skipping ontology refresh.");
            return;
        }

        Map<String, List<String>> sectorNews = groupNewsBySector(taggedNews);

        int totalEntries = 0;
        LocalDate validFrom = LocalDate.now();
        LocalDate validUntil = validFrom.plusDays(VALIDITY_DAYS);

        for (var entry : sectorNews.entrySet()) {
            String sector = entry.getKey();
            List<String> headlines = entry.getValue();

            if (headlines.size() < 3) continue; // Not enough data

            try {
                List<DynamicOntologyEntity> entries =
                        extractOntologyForSector(sector, headlines, validFrom, validUntil);

                // Replace old entries for this sector
                dynamicOntologyRepository.deleteBySector(sector);
                dynamicOntologyRepository.saveAll(entries);
                totalEntries += entries.size();

                log.debug("Extracted {} ontology entries for sector: {}", entries.size(), sector);
            } catch (Exception e) {
                log.error("Failed to extract ontology for sector {}: {}", sector, e.getMessage());
            }
        }

        // Clean up expired entries
        int deleted = dynamicOntologyRepository.deleteExpired(LocalDate.now());

        log.info("Dynamic ontology refresh completed: {} entries created, {} expired deleted",
                totalEntries, deleted);
    }

    private List<NewsArticleEntity> fetchRecentTaggedNews(LocalDateTime since) {
        // Fetch news that have tagged stocks and were published recently
        try {
            return newsArticleRepository.findAllOrderByPublishedAtDesc(PageRequest.of(0, 300))
                    .getContent().stream()
                    .filter(n -> n.getPublishedAt() != null && n.getPublishedAt().isAfter(since))
                    .filter(n -> n.getTaggedStocks() != null && !n.getTaggedStocks().isEmpty())
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch recent tagged news: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, List<String>> groupNewsBySector(List<NewsArticleEntity> articles) {
        Map<String, List<String>> sectorNews = new LinkedHashMap<>();

        for (NewsArticleEntity article : articles) {
            Set<String> sectors = article.getTaggedStocks().stream()
                    .map(StockEntity::getSector)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (String sector : sectors) {
                sectorNews.computeIfAbsent(sector, k -> new ArrayList<>());
                List<String> headlines = sectorNews.get(sector);
                if (headlines.size() < MAX_NEWS_PER_SECTOR) {
                    headlines.add(article.getTitle());
                }
            }
        }

        return sectorNews;
    }

    private List<DynamicOntologyEntity> extractOntologyForSector(
            String sector, List<String> headlines,
            LocalDate validFrom, LocalDate validUntil) {

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append(String.format("[%s 섹터 최근 7일 뉴스 헤드라인]\n", sector));
        for (int i = 0; i < headlines.size(); i++) {
            userPrompt.append(String.format("%d. %s\n", i + 1, headlines.get(i)));
        }
        userPrompt.append("\n위 뉴스를 분석하여 이 섹터의 현재 시장 지식을 JSON으로 추출하세요.");

        String response = openAiClient.chat(SYSTEM_PROMPT, userPrompt.toString(), 800);

        return parseOntologyResponse(sector, response, validFrom, validUntil);
    }

    private List<DynamicOntologyEntity> parseOntologyResponse(
            String sector, String response,
            LocalDate validFrom, LocalDate validUntil) {

        List<DynamicOntologyEntity> entries = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(response);

            // Parse hot issues
            JsonNode hotIssues = root.path("hotIssues");
            if (hotIssues.isArray()) {
                for (JsonNode issue : hotIssues) {
                    String subject = issue.path("issue").asText("");
                    String impact = issue.path("impact").asText("");
                    String stocks = extractStockList(issue.path("relatedStocks"));

                    if (!subject.isEmpty()) {
                        entries.add(new DynamicOntologyEntity(
                                "HOT_ISSUE", sector, subject, impact, stocks,
                                validFrom, validUntil));
                    }
                }
            }

            // Parse stock relations
            JsonNode relations = root.path("stockRelations");
            if (relations.isArray()) {
                for (JsonNode rel : relations) {
                    String from = rel.path("from").asText("");
                    String to = rel.path("to").asText("");
                    String type = rel.path("type").asText("");
                    String desc = rel.path("description").asText("");

                    if (!from.isEmpty() && !to.isEmpty()) {
                        String subject = from + " → " + to + " (" + type + ")";
                        String stocks = from + "," + to;
                        entries.add(new DynamicOntologyEntity(
                                "STOCK_RELATION", sector, subject, desc, stocks,
                                validFrom, validUntil));
                    }
                }
            }

            // Parse emerging themes
            JsonNode themes = root.path("emergingThemes");
            if (themes.isArray()) {
                for (JsonNode theme : themes) {
                    String name = theme.path("theme").asText("");
                    String desc = theme.path("description").asText("");
                    String stocks = extractStockList(theme.path("stocks"));

                    if (!name.isEmpty()) {
                        entries.add(new DynamicOntologyEntity(
                                "EMERGING_THEME", sector, name, desc, stocks,
                                validFrom, validUntil));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse ontology response for sector {}: {}", sector, e.getMessage());
        }

        return entries;
    }

    private String extractStockList(JsonNode arrayNode) {
        if (!arrayNode.isArray()) return "";
        List<String> stocks = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            stocks.add(node.asText());
        }
        return String.join(",", stocks);
    }
}
