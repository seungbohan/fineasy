package com.fineasy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.dto.response.DisclosureSummaryResponse;
import com.fineasy.dto.response.DomesticDisclosureResponse;
import com.fineasy.dto.response.OverseasDisclosureResponse;
import com.fineasy.entity.DartCorpCodeEntity;
import com.fineasy.exception.AiServiceUnavailableException;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.external.dart.DartApiClient;
import com.fineasy.external.openai.OpenAiClient;
import com.fineasy.external.openai.OpenAiPromptBuilder;
import com.fineasy.external.sec.SecEdgarApiClient;
import com.fineasy.repository.DartCorpCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DisclosureService {

    private static final Logger log = LoggerFactory.getLogger(DisclosureService.class);

    private static final DateTimeFormatter DART_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DART_URL_TEMPLATE = "https://dart.fss.or.kr/dsaf001/main.do?rcept_no=%s";
    private static final String SEC_FILING_URL_TEMPLATE =
            "https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK=%s&type=%s&dateb=&owner=include&count=20";

    private static final int DEFAULT_PAGE_COUNT = 20;

    private static final Duration DOMESTIC_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration OVERSEAS_CACHE_TTL = Duration.ofHours(1);
    private static final Duration SUMMARY_CACHE_TTL = Duration.ofHours(24);
    private static final String DOMESTIC_CACHE_PREFIX = "disclosure:domestic:";
    private static final String OVERSEAS_CACHE_PREFIX = "disclosure:overseas:";
    private static final String SUMMARY_CACHE_PREFIX = "disclosure:summary:";
    private static final int MAX_TOKENS_DISCLOSURE_SUMMARY = 1000;

    private final DartCorpCodeRepository dartCorpCodeRepository;
    private final DartApiClient dartApiClient;
    private final SecEdgarApiClient secEdgarApiClient;
    private final RedisCacheHelper redisCacheHelper;
    private final OpenAiClient openAiClient;
    private final OpenAiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public DisclosureService(DartCorpCodeRepository dartCorpCodeRepository,
                              @Autowired(required = false) DartApiClient dartApiClient,
                              @Autowired(required = false) SecEdgarApiClient secEdgarApiClient,
                              RedisCacheHelper redisCacheHelper,
                              @Autowired(required = false) OpenAiClient openAiClient,
                              @Autowired(required = false) OpenAiPromptBuilder promptBuilder,
                              ObjectMapper objectMapper) {
        this.dartCorpCodeRepository = dartCorpCodeRepository;
        this.dartApiClient = dartApiClient;
        this.secEdgarApiClient = secEdgarApiClient;
        this.redisCacheHelper = redisCacheHelper;
        this.openAiClient = openAiClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch DART disclosure list for a domestic stock.
     */
    public DomesticDisclosureResponse getDomesticDisclosures(String stockCode) {
        String cacheKey = DOMESTIC_CACHE_PREFIX + stockCode;
        DomesticDisclosureResponse cached = redisCacheHelper.getFromCache(
                cacheKey, DomesticDisclosureResponse.class);
        if (cached != null) {
            log.debug("Domestic disclosure cache hit for stockCode={}", stockCode);
            return cached;
        }

        if (dartApiClient == null) {
            log.warn("DartApiClient is not available (DART_API_KEY not set)");
            return new DomesticDisclosureResponse(stockCode, "", 0, List.of());
        }

        DartCorpCodeEntity corpCodeEntity = dartCorpCodeRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new EntityNotFoundException("DartCorpCode", stockCode));

        String corpCode = corpCodeEntity.getCorpCode();
        String corpName = corpCodeEntity.getCorpName();

        LocalDate endDate = LocalDate.now();
        LocalDate beginDate = endDate.minusMonths(6);
        String endStr = endDate.format(DART_DATE_FMT);
        String beginStr = beginDate.format(DART_DATE_FMT);

        JsonNode result = dartApiClient.fetchDisclosureList(
                corpCode, beginStr, endStr, 1, DEFAULT_PAGE_COUNT);

        if (result == null) {
            return new DomesticDisclosureResponse(stockCode, corpName, 0, List.of());
        }

        List<DomesticDisclosureResponse.DisclosureItem> items = parseDartDisclosures(result);
        int totalCount = result.path("total_count").asInt(items.size());

        DomesticDisclosureResponse response = new DomesticDisclosureResponse(
                stockCode, corpName, totalCount, items);

        redisCacheHelper.putToCache(cacheKey, response, DOMESTIC_CACHE_TTL);
        return response;
    }

    /**
     * Fetch SEC EDGAR filings for an overseas stock.
     */
    public OverseasDisclosureResponse getOverseasDisclosures(String stockCode) {
        String cacheKey = OVERSEAS_CACHE_PREFIX + stockCode;
        OverseasDisclosureResponse cached = redisCacheHelper.getFromCache(
                cacheKey, OverseasDisclosureResponse.class);
        if (cached != null) {
            log.debug("Overseas disclosure cache hit for stockCode={}", stockCode);
            return cached;
        }

        if (secEdgarApiClient == null) {
            log.warn("SecEdgarApiClient is not available");
            return new OverseasDisclosureResponse(stockCode, "", 0, List.of());
        }

        String cik = secEdgarApiClient.resolveCik(stockCode);
        if (cik == null) {
            log.warn("CIK not found for ticker={}", stockCode);
            return new OverseasDisclosureResponse(stockCode, "", 0, List.of());
        }

        JsonNode filingsRoot = secEdgarApiClient.fetchFilings(cik);
        if (filingsRoot == null) {
            return new OverseasDisclosureResponse(stockCode, "", 0, List.of());
        }

        String companyName = filingsRoot.path("name").asText(stockCode);
        List<OverseasDisclosureResponse.FilingItem> items = parseSecFilings(filingsRoot, cik);

        OverseasDisclosureResponse response = new OverseasDisclosureResponse(
                stockCode, companyName, items.size(), items);

        redisCacheHelper.putToCache(cacheKey, response, OVERSEAS_CACHE_TTL);
        return response;
    }

    private List<DomesticDisclosureResponse.DisclosureItem> parseDartDisclosures(JsonNode result) {
        List<DomesticDisclosureResponse.DisclosureItem> items = new ArrayList<>();
        JsonNode list = result.path("list");
        if (!list.isArray()) return items;

        for (JsonNode node : list) {
            String receiptNo = node.path("rcept_no").asText("");
            String reportName = node.path("report_nm").asText("");
            String filerName = node.path("flr_nm").asText("");
            String receiptDate = node.path("rcept_dt").asText("");

            String disclosureType = classifyDisclosureType(reportName);

            // Filter: skip minor/unimportant disclosures
            if ("기타".equals(disclosureType)) continue;

            String dartUrl = String.format(DART_URL_TEMPLATE, receiptNo);

            items.add(new DomesticDisclosureResponse.DisclosureItem(
                    receiptNo, reportName, filerName, receiptDate, disclosureType, dartUrl));
        }

        // DART API returns newest first, but ensure sort by date descending
        items.sort((a, b) -> b.receiptDate().compareTo(a.receiptDate()));

        return items;
    }

    /**
     * Classify disclosure type based on report name for user-friendly display.
     * Returns "기타" for unimportant disclosures that should be filtered out.
     */
    private String classifyDisclosureType(String reportName) {
        if (reportName == null || reportName.isBlank()) return "기타";

        // 정기공시: 사업보고서, 반기보고서, 분기보고서
        if (reportName.contains("사업보고서")) return "정기공시";
        if (reportName.contains("반기보고서")) return "정기공시";
        if (reportName.contains("분기보고서")) return "정기공시";

        // 주요사항보고
        if (reportName.contains("주요사항보고")) return "주요사항";
        if (reportName.contains("주요경영사항")) return "주요사항";
        if (reportName.contains("풍문또는보도")) return "주요사항";
        if (reportName.contains("공정공시")) return "주요사항";
        if (reportName.contains("조회공시")) return "주요사항";

        // 자본변동/발행공시
        if (reportName.contains("유상증자")) return "자본변동";
        if (reportName.contains("무상증자")) return "자본변동";
        if (reportName.contains("전환사채")) return "자본변동";
        if (reportName.contains("신주인수권")) return "자본변동";
        if (reportName.contains("합병")) return "자본변동";
        if (reportName.contains("분할")) return "자본변동";
        if (reportName.contains("자기주식")) return "자본변동";
        if (reportName.contains("교환사채")) return "자본변동";
        if (reportName.contains("감자")) return "자본변동";
        if (reportName.contains("주식소각")) return "자본변동";

        // 지분변동
        if (reportName.contains("소유상황보고")) return "지분변동";
        if (reportName.contains("대량보유상황")) return "지분변동";
        if (reportName.contains("임원ㆍ주요주주")) return "지분변동";

        // 경영권 관련
        if (reportName.contains("배당")) return "배당";
        if (reportName.contains("결산실적")) return "실적공시";

        // 기재정정은 원본이 중요한 경우만 포함 (정기공시 정정 등)
        if (reportName.contains("기재정정")) {
            if (reportName.contains("사업보고서") || reportName.contains("반기보고서")
                    || reportName.contains("분기보고서") || reportName.contains("주요사항")) {
                return "정정공시";
            }
            return "기타";
        }

        // 감사보고서
        if (reportName.contains("감사보고서")) return "감사보고";

        // 나머지는 기타 (필터링 대상)
        return "기타";
    }

    /**
     * Generate AI summary for a domestic disclosure by receipt number.
     */
    public DisclosureSummaryResponse getDisclosureSummary(String stockCode, String receiptNumber) {
        String cacheKey = SUMMARY_CACHE_PREFIX + receiptNumber;

        DisclosureSummaryResponse cached = redisCacheHelper.getFromCache(
                cacheKey, DisclosureSummaryResponse.class);
        if (cached != null) {
            log.debug("Disclosure summary cache hit for receiptNumber={}", receiptNumber);
            return cached;
        }

        // Find the disclosure in the list to get metadata
        DomesticDisclosureResponse disclosureList = getDomesticDisclosures(stockCode);
        DomesticDisclosureResponse.DisclosureItem target = disclosureList.disclosures().stream()
                .filter(d -> d.receiptNumber().equals(receiptNumber))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Disclosure", receiptNumber));

        if (openAiClient == null || promptBuilder == null) {
            throw new AiServiceUnavailableException(
                    "AI service is not configured. Please set openai.api-key.");
        }

        try {
            String systemPrompt = promptBuilder.getDisclosureSummarySystemPrompt();
            String userPrompt = promptBuilder.buildDisclosureSummaryPrompt(
                    disclosureList.corpName(),
                    target.reportName(),
                    target.filerName(),
                    target.receiptDate(),
                    target.disclosureType());

            String aiResponse = openAiClient.chat(systemPrompt, userPrompt, MAX_TOKENS_DISCLOSURE_SUMMARY);
            DisclosureSummaryResponse response = parseDisclosureSummaryResponse(target, aiResponse);

            redisCacheHelper.putToCache(cacheKey, response, SUMMARY_CACHE_TTL);
            return response;
        } catch (AiServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate disclosure summary for receiptNumber={}: {}",
                    receiptNumber, e.getMessage(), e);
            throw new AiServiceUnavailableException(
                    "AI disclosure summary service is temporarily unavailable.", e);
        }
    }

    private DisclosureSummaryResponse parseDisclosureSummaryResponse(
            DomesticDisclosureResponse.DisclosureItem item, String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            String overview = root.path("overview").asText("공시 요약을 생성할 수 없습니다.");
            String keyPoints = root.path("keyPoints").asText("핵심 내용을 분석할 수 없습니다.");

            List<String> highlights = List.of();
            if (root.has("highlights") && root.get("highlights").isArray()) {
                highlights = objectMapper.convertValue(root.get("highlights"),
                        new TypeReference<List<String>>() {});
            }

            String investorImplication = root.path("investorImplication").asText("");

            String dartUrl = String.format(DART_URL_TEMPLATE, item.receiptNumber());

            return new DisclosureSummaryResponse(
                    item.receiptNumber(),
                    item.reportName(),
                    item.filerName(),
                    item.receiptDate(),
                    item.disclosureType(),
                    dartUrl,
                    new DisclosureSummaryResponse.DisclosureSummary(
                            overview,
                            keyPoints,
                            highlights,
                            investorImplication,
                            DisclosureSummaryResponse.AI_DISCLAIMER
                    )
            );
        } catch (Exception e) {
            log.error("Failed to parse disclosure summary AI response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse disclosure summary", e);
        }
    }

    private List<OverseasDisclosureResponse.FilingItem> parseSecFilings(JsonNode root, String cik) {
        List<OverseasDisclosureResponse.FilingItem> items = new ArrayList<>();

        JsonNode recentFilings = root.path("filings").path("recent");
        if (recentFilings.isMissingNode()) return items;

        JsonNode accessionNumbers = recentFilings.path("accessionNumber");
        JsonNode forms = recentFilings.path("form");
        JsonNode filingDates = recentFilings.path("filingDate");
        JsonNode primaryDocDescriptions = recentFilings.path("primaryDocDescription");

        if (!accessionNumbers.isArray()) return items;

        int limit = Math.min(accessionNumbers.size(), DEFAULT_PAGE_COUNT);
        for (int i = 0; i < limit; i++) {
            String accessionNumber = accessionNumbers.path(i).asText("");
            String form = forms.path(i).asText("");
            String filingDate = filingDates.path(i).asText("");
            String description = primaryDocDescriptions.path(i).asText(form);

            // Build SEC filing URL
            String accessionFormatted = accessionNumber.replace("-", "");
            String secUrl = String.format(
                    "https://www.sec.gov/Archives/edgar/data/%s/%s",
                    cik.replaceFirst("^0+", ""), accessionFormatted);

            items.add(new OverseasDisclosureResponse.FilingItem(
                    accessionNumber, form, filingDate, description, secUrl));
        }

        return items;
    }
}
