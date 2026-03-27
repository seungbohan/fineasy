package com.fineasy.external.sec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fineasy.dto.response.DartFundamentalsResponse;
import com.fineasy.dto.response.MultiYearFundamentalsResponse;
import com.fineasy.service.RedisCacheHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses SEC EDGAR companyfacts XBRL data to produce fundamentals
 * responses for overseas (NASDAQ/NYSE/AMEX) stocks.
 * Reuses the same DTOs as DartFinancialService for a unified API surface.
 */
@Service
public class SecEdgarFundamentalsService {

    private static final Logger log = LoggerFactory.getLogger(SecEdgarFundamentalsService.class);

    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final String CACHE_KEY_PREFIX = "sec:fundamentals:";
    private static final String MULTI_CACHE_KEY_PREFIX = "sec:multi-fundamentals:";

    // --- US-GAAP concept name alternatives (ordered by priority) ---

    private static final String[] REVENUE_CONCEPTS = {
            "Revenues",
            "RevenueFromContractWithCustomerExcludingAssessedTax",
            "SalesRevenueNet"
    };

    private static final String[] OPERATING_INCOME_CONCEPTS = {
            "OperatingIncomeLoss"
    };

    private static final String[] NET_INCOME_CONCEPTS = {
            "NetIncomeLoss"
    };

    private static final String[] TOTAL_ASSETS_CONCEPTS = {
            "Assets"
    };

    private static final String[] TOTAL_LIABILITIES_CONCEPTS = {
            "Liabilities"
    };

    private static final String[] TOTAL_EQUITY_CONCEPTS = {
            "StockholdersEquity",
            "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest"
    };

    private static final String[] OPERATING_CASH_FLOW_CONCEPTS = {
            "NetCashProvidedByUsedInOperatingActivities"
    };

    // --- Evaluation thresholds (aligned with DartFinancialService) ---

    private static final double PROFITABILITY_GOOD_THRESHOLD = 10.0;
    private static final double PROFITABILITY_NORMAL_THRESHOLD = 5.0;
    private static final double ROE_GOOD_THRESHOLD = 10.0;
    private static final double ROE_NORMAL_THRESHOLD = 5.0;
    private static final double DEBT_STABLE_THRESHOLD = 100.0;
    private static final double DEBT_NORMAL_THRESHOLD = 200.0;
    private static final double GROWTH_HIGH_THRESHOLD = 10.0;

    private final SecEdgarApiClient secEdgarApiClient;
    private final RedisCacheHelper redisCacheHelper;

    public SecEdgarFundamentalsService(SecEdgarApiClient secEdgarApiClient,
                                        RedisCacheHelper redisCacheHelper) {
        this.secEdgarApiClient = secEdgarApiClient;
        this.redisCacheHelper = redisCacheHelper;
        log.info("SecEdgarFundamentalsService bean created successfully");
    }

    /**
     * Returns the latest annual fundamentals for an overseas stock.
     */
    public DartFundamentalsResponse getFundamentals(String stockCode, String stockName) {
        String cacheKey = CACHE_KEY_PREFIX + stockCode;
        DartFundamentalsResponse cached = redisCacheHelper.getFromCache(cacheKey, DartFundamentalsResponse.class);
        if (cached != null) {
            log.debug("SEC fundamentals cache hit for stockCode={}", stockCode);
            return cached;
        }

        JsonNode companyFacts = fetchCompanyFacts(stockCode);
        if (companyFacts == null) {
            return null;
        }

        List<Integer> years = getAvailableFiscalYears(companyFacts);
        if (years.isEmpty()) {
            log.info("No annual fiscal years found in SEC data for stockCode={}", stockCode);
            return null;
        }

        int latestYear = years.get(0);
        Integer priorYear = years.size() > 1 ? years.get(1) : null;

        DartFundamentalsResponse response = buildSingleYearResponse(
                companyFacts, stockCode, stockName, latestYear, priorYear);

        if (response != null) {
            redisCacheHelper.putToCache(cacheKey, response, CACHE_TTL);
        }
        return response;
    }

    /**
     * Returns up to 5 years of annual fundamentals for an overseas stock.
     */
    public MultiYearFundamentalsResponse getMultiYearFundamentals(String stockCode, String stockName) {
        String cacheKey = MULTI_CACHE_KEY_PREFIX + stockCode;
        MultiYearFundamentalsResponse cached = redisCacheHelper.getFromCache(
                cacheKey, MultiYearFundamentalsResponse.class);
        if (cached != null) {
            log.debug("SEC multi-year fundamentals cache hit for stockCode={}", stockCode);
            return cached;
        }

        JsonNode companyFacts = fetchCompanyFacts(stockCode);
        if (companyFacts == null) {
            return null;
        }

        List<Integer> years = getAvailableFiscalYears(companyFacts);
        if (years.isEmpty()) {
            log.info("No annual fiscal years found in SEC data for stockCode={}", stockCode);
            return null;
        }

        // Take up to 5 most recent years
        List<Integer> targetYears = years.subList(0, Math.min(5, years.size()));
        List<MultiYearFundamentalsResponse.YearlyData> yearlyDataList = new ArrayList<>();

        for (int i = 0; i < targetYears.size(); i++) {
            int fy = targetYears.get(i);
            Integer priorFy = (i + 1 < targetYears.size()) ? targetYears.get(i + 1) : findPriorYear(years, fy);

            Long revenue = extractAnnualFact(companyFacts, fy, REVENUE_CONCEPTS);
            Long operatingProfit = extractAnnualFact(companyFacts, fy, OPERATING_INCOME_CONCEPTS);
            Long netIncome = extractAnnualFact(companyFacts, fy, NET_INCOME_CONCEPTS);
            Long totalAssets = extractAnnualFact(companyFacts, fy, TOTAL_ASSETS_CONCEPTS);
            Long totalLiabilities = extractAnnualFact(companyFacts, fy, TOTAL_LIABILITIES_CONCEPTS);
            Long totalEquity = extractAnnualFact(companyFacts, fy, TOTAL_EQUITY_CONCEPTS);

            if (revenue == null && totalAssets == null && netIncome == null) {
                continue;
            }

            Double operatingMargin = computeOperatingMargin(operatingProfit, revenue);
            Double roe = computeRoe(netIncome, totalEquity);
            Double debtRatio = computeDebtRatio(totalLiabilities, totalEquity);

            Double revenueGrowthRate = null;
            if (priorFy != null) {
                Long priorRevenue = extractAnnualFact(companyFacts, priorFy, REVENUE_CONCEPTS);
                revenueGrowthRate = computeRevenueGrowthRate(revenue, priorRevenue);
            }

            yearlyDataList.add(new MultiYearFundamentalsResponse.YearlyData(
                    String.valueOf(fy),
                    revenue, operatingProfit, netIncome,
                    operatingMargin, roe, debtRatio, revenueGrowthRate
            ));
        }

        if (yearlyDataList.isEmpty()) {
            log.info("No multi-year SEC data available for stockCode={}", stockCode);
            return null;
        }

        // Already sorted descending from getAvailableFiscalYears, but ensure it
        yearlyDataList.sort(Comparator.comparing(MultiYearFundamentalsResponse.YearlyData::bsnsYear).reversed());

        MultiYearFundamentalsResponse response = new MultiYearFundamentalsResponse(
                stockCode, stockName, yearlyDataList);

        redisCacheHelper.putToCache(cacheKey, response, CACHE_TTL);
        return response;
    }

    // ===== Internal helpers =====

    private JsonNode fetchCompanyFacts(String stockCode) {
        String cik = secEdgarApiClient.resolveCik(stockCode);
        if (cik == null) {
            log.debug("Could not resolve CIK for stockCode={}", stockCode);
            return null;
        }

        JsonNode companyFacts = secEdgarApiClient.fetchCompanyFacts(cik);
        if (companyFacts == null) {
            log.debug("No company facts returned for stockCode={} (CIK={})", stockCode, cik);
            return null;
        }
        return companyFacts;
    }

    /**
     * Extract the annual (10-K) value for the given fiscal year,
     * trying multiple concept names in priority order.
     */
    private Long extractAnnualFact(JsonNode companyFacts, int fiscalYear, String... conceptNames) {
        for (String concept : conceptNames) {
            JsonNode units = companyFacts.path("facts").path("us-gaap")
                    .path(concept).path("units").path("USD");
            if (!units.isArray()) continue;

            Long bestVal = null;
            String bestEnd = "";

            for (JsonNode entry : units) {
                int fy = entry.path("fy").asInt(0);
                String fp = entry.path("fp").asText("");
                String form = entry.path("form").asText("");

                // Annual data only: FY period + 10-K or 10-K/A form
                if (fy == fiscalYear && "FY".equals(fp)
                        && ("10-K".equals(form) || "10-K/A".equals(form))) {
                    String end = entry.path("end").asText("");
                    long val = entry.path("val").asLong(0);
                    // Pick the entry with the latest end date (handles amendments)
                    if (end.compareTo(bestEnd) > 0) {
                        bestEnd = end;
                        bestVal = val;
                    }
                }
            }

            if (bestVal != null) return bestVal;
        }
        return null;
    }

    /**
     * Collect available fiscal years from the companyfacts data.
     * Looks at Revenue or Assets concepts, returns descending sorted unique years.
     */
    private List<Integer> getAvailableFiscalYears(JsonNode companyFacts) {
        Set<Integer> years = new TreeSet<>(Comparator.reverseOrder());

        // Scan revenue concepts first, then assets as fallback
        String[][] conceptGroups = {REVENUE_CONCEPTS, TOTAL_ASSETS_CONCEPTS};

        for (String[] concepts : conceptGroups) {
            for (String concept : concepts) {
                JsonNode units = companyFacts.path("facts").path("us-gaap")
                        .path(concept).path("units").path("USD");
                if (!units.isArray()) continue;

                for (JsonNode entry : units) {
                    String fp = entry.path("fp").asText("");
                    String form = entry.path("form").asText("");
                    int fy = entry.path("fy").asInt(0);

                    if ("FY".equals(fp) && ("10-K".equals(form) || "10-K/A".equals(form)) && fy > 0) {
                        years.add(fy);
                    }
                }
            }
            if (!years.isEmpty()) break; // Found years from revenue concepts
        }

        return new ArrayList<>(years);
    }

    private Integer findPriorYear(List<Integer> sortedYears, int currentYear) {
        for (int y : sortedYears) {
            if (y < currentYear) return y;
        }
        return null;
    }

    private DartFundamentalsResponse buildSingleYearResponse(
            JsonNode companyFacts, String stockCode, String stockName,
            int fiscalYear, Integer priorFiscalYear) {

        Long revenue = extractAnnualFact(companyFacts, fiscalYear, REVENUE_CONCEPTS);
        Long operatingProfit = extractAnnualFact(companyFacts, fiscalYear, OPERATING_INCOME_CONCEPTS);
        Long netIncome = extractAnnualFact(companyFacts, fiscalYear, NET_INCOME_CONCEPTS);
        Long totalAssets = extractAnnualFact(companyFacts, fiscalYear, TOTAL_ASSETS_CONCEPTS);
        Long totalLiabilities = extractAnnualFact(companyFacts, fiscalYear, TOTAL_LIABILITIES_CONCEPTS);
        Long totalEquity = extractAnnualFact(companyFacts, fiscalYear, TOTAL_EQUITY_CONCEPTS);
        Long operatingCashFlow = extractAnnualFact(companyFacts, fiscalYear, OPERATING_CASH_FLOW_CONCEPTS);

        if (revenue == null && totalAssets == null && netIncome == null) {
            log.debug("Insufficient SEC data for stockCode={}, fy={}", stockCode, fiscalYear);
            return null;
        }

        Double operatingMargin = computeOperatingMargin(operatingProfit, revenue);
        Double roe = computeRoe(netIncome, totalEquity);
        Double debtRatio = computeDebtRatio(totalLiabilities, totalEquity);

        Double revenueGrowthRate = null;
        if (priorFiscalYear != null) {
            Long priorRevenue = extractAnnualFact(companyFacts, priorFiscalYear, REVENUE_CONCEPTS);
            revenueGrowthRate = computeRevenueGrowthRate(revenue, priorRevenue);
        }

        List<String> evaluationTags = buildEvaluationTags(operatingMargin, roe, debtRatio, revenueGrowthRate);
        String summaryComment = buildSummaryComment(operatingMargin, roe, debtRatio, revenueGrowthRate);

        return new DartFundamentalsResponse(
                stockCode, stockName, String.valueOf(fiscalYear),
                revenue, operatingProfit, netIncome,
                totalAssets, totalLiabilities, totalEquity,
                operatingCashFlow,
                operatingMargin, revenueGrowthRate, debtRatio, roe,
                evaluationTags, summaryComment
        );
    }

    // ===== Ratio computations =====

    private Double computeOperatingMargin(Long operatingProfit, Long revenue) {
        if (operatingProfit == null || revenue == null || revenue == 0) return null;
        return Math.round((double) operatingProfit / revenue * 100 * 100.0) / 100.0;
    }

    private Double computeRevenueGrowthRate(Long currentRevenue, Long priorRevenue) {
        if (currentRevenue == null || priorRevenue == null || priorRevenue == 0) return null;
        return Math.round((double) (currentRevenue - priorRevenue) / priorRevenue * 100 * 100.0) / 100.0;
    }

    private Double computeDebtRatio(Long totalLiabilities, Long totalEquity) {
        if (totalLiabilities == null || totalEquity == null || totalEquity == 0) return null;
        return Math.round((double) totalLiabilities / totalEquity * 100 * 100.0) / 100.0;
    }

    private Double computeRoe(Long netIncome, Long totalEquity) {
        if (netIncome == null || totalEquity == null || totalEquity == 0) return null;
        return Math.round((double) netIncome / totalEquity * 100 * 100.0) / 100.0;
    }

    // ===== Evaluation tags and summary (same criteria as DartFinancialService) =====

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
            String desc;
            if (operatingMargin >= PROFITABILITY_GOOD_THRESHOLD) {
                desc = "양호";
            } else if (operatingMargin >= PROFITABILITY_NORMAL_THRESHOLD) {
                desc = "보통 수준";
            } else {
                desc = "낮은 편";
            }
            parts.add(String.format("영업이익률 %.1f%%로 %s", operatingMargin, desc));
        }

        if (debtRatio != null) {
            String desc;
            if (debtRatio < DEBT_STABLE_THRESHOLD) {
                desc = "재무 안정적";
            } else if (debtRatio < DEBT_NORMAL_THRESHOLD) {
                desc = "보통 수준";
            } else {
                desc = "다소 높은 편";
            }
            parts.add(String.format("부채비율 %.1f%%로 %s", debtRatio, desc));
        }

        if (roe != null) {
            String desc;
            if (roe >= ROE_GOOD_THRESHOLD) {
                desc = "양호한 수준";
            } else if (roe >= ROE_NORMAL_THRESHOLD) {
                desc = "보통 수준";
            } else {
                desc = "낮은 편";
            }
            parts.add(String.format("ROE %.1f%%는 %s", roe, desc));
        }

        if (revenueGrowthRate != null) {
            String desc;
            if (revenueGrowthRate >= GROWTH_HIGH_THRESHOLD) {
                desc = "높은 성장세";
            } else if (revenueGrowthRate >= 0) {
                desc = "안정적인 흐름";
            } else {
                desc = "감소 추세";
            }
            parts.add(String.format("매출성장률 %.1f%%로 %s", revenueGrowthRate, desc));
        }

        if (parts.isEmpty()) {
            return "재무 데이터가 부족하여 종합 판단이 어렵습니다.";
        }

        return String.join(", ", parts) + ".";
    }
}
