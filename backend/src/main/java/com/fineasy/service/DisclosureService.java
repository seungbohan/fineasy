package com.fineasy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fineasy.dto.response.DomesticDisclosureResponse;
import com.fineasy.dto.response.OverseasDisclosureResponse;
import com.fineasy.entity.DartCorpCodeEntity;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.external.dart.DartApiClient;
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
    private static final String DOMESTIC_CACHE_PREFIX = "disclosure:domestic:";
    private static final String OVERSEAS_CACHE_PREFIX = "disclosure:overseas:";

    private final DartCorpCodeRepository dartCorpCodeRepository;
    private final DartApiClient dartApiClient;
    private final SecEdgarApiClient secEdgarApiClient;
    private final RedisCacheHelper redisCacheHelper;

    public DisclosureService(DartCorpCodeRepository dartCorpCodeRepository,
                              @Autowired(required = false) DartApiClient dartApiClient,
                              @Autowired(required = false) SecEdgarApiClient secEdgarApiClient,
                              RedisCacheHelper redisCacheHelper) {
        this.dartCorpCodeRepository = dartCorpCodeRepository;
        this.dartApiClient = dartApiClient;
        this.secEdgarApiClient = secEdgarApiClient;
        this.redisCacheHelper = redisCacheHelper;
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
            String corpCls = node.path("corp_cls").asText("");

            // corp_cls: Y=유가, K=코스닥, N=코넥스, E=기타
            String disclosureType = switch (corpCls) {
                case "Y" -> "유가증권";
                case "K" -> "코스닥";
                case "N" -> "코넥스";
                case "E" -> "기타";
                default -> corpCls;
            };

            String dartUrl = String.format(DART_URL_TEMPLATE, receiptNo);

            items.add(new DomesticDisclosureResponse.DisclosureItem(
                    receiptNo, reportName, filerName, receiptDate, disclosureType, dartUrl));
        }

        return items;
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
