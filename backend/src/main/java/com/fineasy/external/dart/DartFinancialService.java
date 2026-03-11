package com.fineasy.external.dart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fineasy.dto.response.DartFundamentalsResponse;
import com.fineasy.dto.response.MultiYearFundamentalsResponse;
import com.fineasy.entity.DartCorpCodeEntity;
import com.fineasy.entity.DartFinancialEntity;
import com.fineasy.repository.DartCorpCodeRepository;
import com.fineasy.repository.DartFinancialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@ConditionalOnExpression("!'${dart.api.key:}'.isEmpty()")
public class DartFinancialService {

    private static final Logger log = LoggerFactory.getLogger(DartFinancialService.class);

    private static final String ANNUAL_REPORT_CODE = "11011";

    private static final Map<String, List<String>> ACCOUNT_NAME_MAP = Map.of(
            "revenue", List.of("매출액", "영업수익", "수익(매출액)", "매출"),
            "operatingProfit", List.of("영업이익", "영업이익(손실)"),
            "netIncome", List.of("당기순이익", "당기순이익(손실)", "당기순이익(손실)의 귀속"),
            "totalAssets", List.of("자산총계"),
            "totalLiabilities", List.of("부채총계"),
            "totalEquity", List.of("자본총계"),
            "operatingCashFlow", List.of("영업활동현금흐름", "영업활동으로인한현금흐름",
                    "영업활동 현금흐름", "영업활동으로 인한 현금흐름")
    );

    private final DartApiClient dartApiClient;
    private final DartCorpCodeRepository corpCodeRepository;
    private final DartFinancialRepository financialRepository;

    public DartFinancialService(DartApiClient dartApiClient,
                                 DartCorpCodeRepository corpCodeRepository,
                                 DartFinancialRepository financialRepository) {
        this.dartApiClient = dartApiClient;
        this.corpCodeRepository = corpCodeRepository;
        this.financialRepository = financialRepository;
        log.info("DartFinancialService bean created successfully");
    }

    public DartFundamentalsResponse getFundamentals(String stockCode, String stockName) {

        Optional<DartFinancialEntity> cached = financialRepository
                .findTopByStockCodeOrderByBsnsYearDesc(stockCode);

        if (cached.isPresent()) {
            DartFinancialEntity entity = cached.get();
            DartFinancialEntity priorYear = findPriorYear(stockCode, entity.getBsnsYear());
            return buildResponse(entity, priorYear, stockName);
        }

        Optional<DartCorpCodeEntity> corpCodeOpt = corpCodeRepository.findByStockCode(stockCode);
        if (corpCodeOpt.isEmpty()) {
            log.debug("No DART corp code found for stockCode={}", stockCode);
            return null;
        }

        String corpCode = corpCodeOpt.get().getCorpCode();

        int currentYear = LocalDate.now().getYear();
        DartFinancialEntity fetched = fetchAndSaveFinancials(stockCode, corpCode, currentYear - 1);

        if (fetched == null) {

            fetched = fetchAndSaveFinancials(stockCode, corpCode, currentYear - 2);
        }

        if (fetched == null) {
            log.info("No DART financial data available for stockCode={}", stockCode);
            return null;
        }

        DartFinancialEntity priorYear = findPriorYear(stockCode, fetched.getBsnsYear());
        if (priorYear == null) {

            int priorYearInt = Integer.parseInt(fetched.getBsnsYear()) - 1;
            priorYear = fetchAndSaveFinancials(stockCode, corpCode, priorYearInt);
        }

        return buildResponse(fetched, priorYear, stockName);
    }

    @Transactional
    DartFinancialEntity fetchAndSaveFinancials(String stockCode, String corpCode, int year) {
        String bsnsYear = String.valueOf(year);

        Optional<DartFinancialEntity> existing = financialRepository
                .findByStockCodeAndBsnsYear(stockCode, bsnsYear);
        if (existing.isPresent()) {
            return existing.get();
        }

        JsonNode response = dartApiClient.fetchSingleCompanyAccount(
                corpCode, bsnsYear, ANNUAL_REPORT_CODE, "CFS");

        if (response == null || !response.has("list")) {

            response = dartApiClient.fetchSingleCompanyAccount(
                    corpCode, bsnsYear, ANNUAL_REPORT_CODE, "OFS");
        }

        if (response == null || !response.has("list")) {
            log.debug("No DART financial data for corpCode={}, year={}", corpCode, bsnsYear);
            return null;
        }

        DartFinancialEntity entity = parseFinancialData(stockCode, bsnsYear, response.get("list"));
        if (entity == null) {
            return null;
        }

        fetchAndSetOperatingCashFlow(entity, corpCode, bsnsYear);

        return financialRepository.save(entity);
    }

    private void fetchAndSetOperatingCashFlow(DartFinancialEntity entity,
                                               String corpCode, String bsnsYear) {
        try {

            JsonNode allResponse = dartApiClient.fetchSingleCompanyAccountAll(
                    corpCode, bsnsYear, ANNUAL_REPORT_CODE, "CFS");

            if (allResponse == null || !allResponse.has("list")) {
                allResponse = dartApiClient.fetchSingleCompanyAccountAll(
                        corpCode, bsnsYear, ANNUAL_REPORT_CODE, "OFS");
            }

            if (allResponse == null || !allResponse.has("list")) {
                log.debug("No fnlttSinglAcntAll data for corpCode={}, year={}", corpCode, bsnsYear);
                return;
            }

            JsonNode listNode = allResponse.get("list");
            List<String> cashFlowNames = ACCOUNT_NAME_MAP.get("operatingCashFlow");

            for (JsonNode item : listNode) {
                String sjDiv = item.path("sj_div").asText("");

                if (!"CF".equals(sjDiv)) {
                    continue;
                }
                String accountNm = item.path("account_nm").asText("");
                if (cashFlowNames.contains(accountNm)) {
                    String amountStr = item.path("thstrm_amount").asText("");
                    Long cashFlow = parseAmount(amountStr);
                    if (cashFlow != null) {
                        entity.setOperatingCashFlow(cashFlow);
                        log.debug("Operating cash flow set for stockCode={}, year={}: {}",
                                entity.getStockCode(), bsnsYear, cashFlow);
                    }
                    return;
                }
            }

            log.debug("Operating cash flow account not found for corpCode={}, year={}", corpCode, bsnsYear);
        } catch (Exception e) {

            log.warn("Failed to fetch operating cash flow for corpCode={}, year={}: {}",
                    corpCode, bsnsYear, e.getMessage());
        }
    }

    public MultiYearFundamentalsResponse getMultiYearFundamentals(String stockCode, String stockName) {

        Optional<DartCorpCodeEntity> corpCodeOpt = corpCodeRepository.findByStockCode(stockCode);
        if (corpCodeOpt.isEmpty()) {
            log.debug("No DART corp code found for stockCode={}", stockCode);
            return null;
        }

        String corpCode = corpCodeOpt.get().getCorpCode();
        int currentYear = LocalDate.now().getYear();

        List<MultiYearFundamentalsResponse.YearlyData> yearlyDataList = new ArrayList<>();

        for (int year = currentYear - 1; year >= currentYear - 5; year--) {
            String bsnsYear = String.valueOf(year);

            Optional<DartFinancialEntity> cached = financialRepository
                    .findByStockCodeAndBsnsYear(stockCode, bsnsYear);

            DartFinancialEntity entity;
            if (cached.isPresent()) {
                entity = cached.get();
            } else {

                entity = fetchAndSaveFinancials(stockCode, corpCode, year);
            }

            if (entity != null) {

                DartFinancialEntity priorYear = findPriorYear(stockCode, entity.getBsnsYear());

                Double operatingMargin = computeOperatingMargin(entity);
                Double roe = computeRoe(entity);
                Double debtRatio = computeDebtRatio(entity);
                Double revenueGrowthRate = computeRevenueGrowthRate(entity, priorYear);

                yearlyDataList.add(new MultiYearFundamentalsResponse.YearlyData(
                        entity.getBsnsYear(),
                        entity.getRevenue(),
                        entity.getOperatingProfit(),
                        entity.getNetIncome(),
                        operatingMargin,
                        roe,
                        debtRatio,
                        revenueGrowthRate
                ));
            }
        }

        if (yearlyDataList.isEmpty()) {
            log.info("No multi-year DART data available for stockCode={}", stockCode);
            return null;
        }

        yearlyDataList.sort(Comparator.comparing(MultiYearFundamentalsResponse.YearlyData::bsnsYear).reversed());

        return new MultiYearFundamentalsResponse(stockCode, stockName, yearlyDataList);
    }

    private DartFinancialEntity parseFinancialData(String stockCode, String bsnsYear,
                                                     JsonNode listNode) {
        if (!listNode.isArray() || listNode.isEmpty()) {
            return null;
        }

        Long revenue = findAccountAmount(listNode, ACCOUNT_NAME_MAP.get("revenue"));
        Long operatingProfit = findAccountAmount(listNode, ACCOUNT_NAME_MAP.get("operatingProfit"));
        Long netIncome = findAccountAmount(listNode, ACCOUNT_NAME_MAP.get("netIncome"));
        Long totalAssets = findAccountAmount(listNode, ACCOUNT_NAME_MAP.get("totalAssets"));
        Long totalLiabilities = findAccountAmount(listNode, ACCOUNT_NAME_MAP.get("totalLiabilities"));
        Long totalEquity = findAccountAmount(listNode, ACCOUNT_NAME_MAP.get("totalEquity"));

        if (revenue == null && totalAssets == null && netIncome == null) {
            log.debug("Insufficient financial data for stockCode={}, year={}", stockCode, bsnsYear);
            return null;
        }

        return new DartFinancialEntity(
                stockCode, bsnsYear,
                revenue, operatingProfit, netIncome,
                totalAssets, totalLiabilities, totalEquity,
                null
        );
    }

    private Long findAccountAmount(JsonNode listNode, List<String> accountNames) {
        for (JsonNode item : listNode) {
            String accountNm = item.path("account_nm").asText("");
            if (accountNames.contains(accountNm)) {
                String amountStr = item.path("thstrm_amount").asText("");
                return parseAmount(amountStr);
            }
        }
        return null;
    }

    private Long parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank() || "-".equals(amountStr.trim())) {
            return null;
        }
        try {
            String cleaned = amountStr.replaceAll("[,\\s]", "");
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            log.debug("Failed to parse DART amount: '{}'", amountStr);
            return null;
        }
    }

    private DartFinancialEntity findPriorYear(String stockCode, String currentYear) {
        try {
            int priorYear = Integer.parseInt(currentYear) - 1;
            return financialRepository
                    .findByStockCodeAndBsnsYear(stockCode, String.valueOf(priorYear))
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private DartFundamentalsResponse buildResponse(DartFinancialEntity entity,
                                                     DartFinancialEntity priorYear,
                                                     String stockName) {
        Double operatingMargin = computeOperatingMargin(entity);
        Double revenueGrowthRate = computeRevenueGrowthRate(entity, priorYear);
        Double debtRatio = computeDebtRatio(entity);
        Double roe = computeRoe(entity);

        List<String> evaluationTags = buildEvaluationTags(operatingMargin, roe, debtRatio, revenueGrowthRate);
        String summaryComment = buildSummaryComment(operatingMargin, roe, debtRatio, revenueGrowthRate);

        return new DartFundamentalsResponse(
                entity.getStockCode(),
                stockName,
                entity.getBsnsYear(),
                entity.getRevenue(),
                entity.getOperatingProfit(),
                entity.getNetIncome(),
                entity.getTotalAssets(),
                entity.getTotalLiabilities(),
                entity.getTotalEquity(),
                entity.getOperatingCashFlow(),
                operatingMargin,
                revenueGrowthRate,
                debtRatio,
                roe,
                evaluationTags,
                summaryComment
        );
    }

    private static final double PROFITABILITY_GOOD_THRESHOLD = 10.0;
    private static final double PROFITABILITY_NORMAL_THRESHOLD = 5.0;
    private static final double ROE_GOOD_THRESHOLD = 10.0;
    private static final double ROE_NORMAL_THRESHOLD = 5.0;
    private static final double DEBT_STABLE_THRESHOLD = 100.0;
    private static final double DEBT_NORMAL_THRESHOLD = 200.0;
    private static final double GROWTH_HIGH_THRESHOLD = 10.0;

    private List<String> buildEvaluationTags(Double operatingMargin, Double roe,
                                              Double debtRatio, Double revenueGrowthRate) {
        List<String> tags = new ArrayList<>();

        if (operatingMargin != null) {
            if (operatingMargin >= PROFITABILITY_GOOD_THRESHOLD) {
                tags.add("수익성 양호");
            } else if (operatingMargin >= PROFITABILITY_NORMAL_THRESHOLD) {
                tags.add("수익성 보통");
            } else {
                tags.add("수익성 주의");
            }
        }

        if (roe != null) {
            if (roe >= ROE_GOOD_THRESHOLD) {
                tags.add("ROE 양호");
            } else if (roe >= ROE_NORMAL_THRESHOLD) {
                tags.add("ROE 보통");
            } else {
                tags.add("ROE 주의");
            }
        }

        if (debtRatio != null) {
            if (debtRatio < DEBT_STABLE_THRESHOLD) {
                tags.add("부채 안정");
            } else if (debtRatio < DEBT_NORMAL_THRESHOLD) {
                tags.add("부채 보통");
            } else {
                tags.add("부채 주의");
            }
        }

        if (revenueGrowthRate != null) {
            if (revenueGrowthRate >= GROWTH_HIGH_THRESHOLD) {
                tags.add("성장세");
            } else if (revenueGrowthRate >= 0) {
                tags.add("안정");
            } else {
                tags.add("매출 감소");
            }
        }

        return tags;
    }

    private String buildSummaryComment(Double operatingMargin, Double roe,
                                        Double debtRatio, Double revenueGrowthRate) {
        List<String> parts = new ArrayList<>();

        if (operatingMargin != null) {
            String profitabilityDesc;
            if (operatingMargin >= PROFITABILITY_GOOD_THRESHOLD) {
                profitabilityDesc = "양호";
            } else if (operatingMargin >= PROFITABILITY_NORMAL_THRESHOLD) {
                profitabilityDesc = "보통 수준";
            } else {
                profitabilityDesc = "낮은 편";
            }
            parts.add(String.format("영업이익률 %.1f%%로 %s", operatingMargin, profitabilityDesc));
        }

        if (debtRatio != null) {
            String debtDesc;
            if (debtRatio < DEBT_STABLE_THRESHOLD) {
                debtDesc = "재무 안정적";
            } else if (debtRatio < DEBT_NORMAL_THRESHOLD) {
                debtDesc = "보통 수준";
            } else {
                debtDesc = "다소 높은 편";
            }
            parts.add(String.format("부채비율 %.1f%%로 %s", debtRatio, debtDesc));
        }

        if (roe != null) {
            String roeDesc;
            if (roe >= ROE_GOOD_THRESHOLD) {
                roeDesc = "양호한 수준";
            } else if (roe >= ROE_NORMAL_THRESHOLD) {
                roeDesc = "보통 수준";
            } else {
                roeDesc = "낮은 편";
            }
            parts.add(String.format("ROE %.1f%%는 %s", roe, roeDesc));
        }

        if (revenueGrowthRate != null) {
            String growthDesc;
            if (revenueGrowthRate >= GROWTH_HIGH_THRESHOLD) {
                growthDesc = "높은 성장세";
            } else if (revenueGrowthRate >= 0) {
                growthDesc = "안정적인 흐름";
            } else {
                growthDesc = "감소 추세";
            }
            parts.add(String.format("매출성장률 %.1f%%로 %s", revenueGrowthRate, growthDesc));
        }

        if (parts.isEmpty()) {
            return "재무 데이터가 부족하여 종합 판단이 어렵습니다.";
        }

        return String.join(", ", parts) + ".";
    }

    private Double computeOperatingMargin(DartFinancialEntity entity) {
        if (entity.getOperatingProfit() == null || entity.getRevenue() == null
                || entity.getRevenue() == 0) {
            return null;
        }
        return Math.round(
                (double) entity.getOperatingProfit() / entity.getRevenue() * 100 * 100.0
        ) / 100.0;
    }

    private Double computeRevenueGrowthRate(DartFinancialEntity current,
                                              DartFinancialEntity prior) {
        if (current.getRevenue() == null || prior == null || prior.getRevenue() == null
                || prior.getRevenue() == 0) {
            return null;
        }
        return Math.round(
                (double) (current.getRevenue() - prior.getRevenue()) / prior.getRevenue() * 100 * 100.0
        ) / 100.0;
    }

    private Double computeDebtRatio(DartFinancialEntity entity) {
        if (entity.getTotalLiabilities() == null || entity.getTotalEquity() == null
                || entity.getTotalEquity() == 0) {
            return null;
        }
        return Math.round(
                (double) entity.getTotalLiabilities() / entity.getTotalEquity() * 100 * 100.0
        ) / 100.0;
    }

    private Double computeRoe(DartFinancialEntity entity) {
        if (entity.getNetIncome() == null || entity.getTotalEquity() == null
                || entity.getTotalEquity() == 0) {
            return null;
        }
        return Math.round(
                (double) entity.getNetIncome() / entity.getTotalEquity() * 100 * 100.0
        ) / 100.0;
    }
}
