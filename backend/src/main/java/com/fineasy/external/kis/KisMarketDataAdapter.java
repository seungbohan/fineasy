package com.fineasy.external.kis;

import com.fineasy.dto.response.StockRankingResponse;
import com.fineasy.service.MarketDataProvider;
import com.fineasy.entity.MarketIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static com.fineasy.external.kis.KisResponseParser.*;

@Component
@Primary
@ConditionalOnProperty(name = "data-provider.type", havingValue = "kis")
public class KisMarketDataAdapter implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(KisMarketDataAdapter.class);

    private static final String INQUIRE_INDEX_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-index-price";
    private static final String INQUIRE_DAILY_INDEX_PATH = "/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice";

    private static final String FLUCTUATION_RANKING_PATH = "/uapi/domestic-stock/v1/ranking/fluctuation";
    private static final String TR_ID_FLUCTUATION_RANKING = "FHPST01700000";

    private static final String VOLUME_RANKING_PATH = "/uapi/domestic-stock/v1/quotations/volume-rank";
    private static final String TR_ID_VOLUME_RANKING = "FHPST01710000";

    private static final String TR_ID_INDEX_PRICE = "FHPUP02100000";
    private static final String TR_ID_DAILY_INDEX = "FHKUP03500100";

    private static final String OVERSEAS_INDEX_PRICE_PATH = "/uapi/overseas-price/v1/quotations/inquire-daily-chartprice";
    private static final String TR_ID_OVERSEAS_INDEX = "FHKST03030100";

    private static final List<IndexConfig> DOMESTIC_INDICES = List.of(
            new IndexConfig("0001", "KOSPI", "코스피"),
            new IndexConfig("1001", "KOSDAQ", "코스닥")
    );

    private static final List<OverseasIndexConfig> OVERSEAS_INDICES = List.of(
            new OverseasIndexConfig("NAS", "COMP", "NASDAQ", "나스닥"),
            new OverseasIndexConfig("NYS", "SPX", "SP500", "S&P 500"),
            new OverseasIndexConfig("NYS", ".DJI", "DJI", "다우존스"),
            new OverseasIndexConfig("NAS", "SOX", "SOX", "필라델피아반도체")
    );

    private final KisApiClient apiClient;

    public KisMarketDataAdapter(KisApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<MarketIndex> getMarketIndices() {
        log.debug("Fetching market indices from KIS API");

        List<MarketIndex> indices = new ArrayList<>();

        for (IndexConfig config : DOMESTIC_INDICES) {
            try {
                MarketIndex index = fetchSingleIndex(config);
                indices.add(index);
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("Failed to fetch index {} ({}): {}", config.displayCode(), config.name(), e.getMessage());
                indices.add(new MarketIndex(
                        config.displayCode(), config.name(),
                        0.0, 0.0, 0.0,
                        List.of(), Instant.now()
                ));
            }
        }

        for (OverseasIndexConfig config : OVERSEAS_INDICES) {
            try {
                MarketIndex index = fetchOverseasIndex(config);
                indices.add(index);
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("Failed to fetch overseas index {} ({}): {}", config.displayCode(), config.name(), e.getMessage());
                indices.add(new MarketIndex(
                        config.displayCode(), config.name(),
                        0.0, 0.0, 0.0,
                        List.of(), Instant.now()
                ));
            }
        }

        return Collections.unmodifiableList(indices);
    }

    @Override
    public String getMarketSummary() {
        log.debug("Generating market summary from KIS API data");

        List<MarketIndex> indices = getMarketIndices();

        StringBuilder summary = new StringBuilder();
        summary.append("현재 국내 증시 현황: ");

        for (MarketIndex index : indices) {
            String direction = index.changeAmount() >= 0 ? "상승" : "하락";
            summary.append(String.format("%s %,.2f (%s %.2f%%), ",
                    index.name(), index.currentValue(), direction, Math.abs(index.changeRate())));
        }

        if (summary.length() > 2) {
            summary.setLength(summary.length() - 2);
        }

        return summary.toString();
    }

    @Override
    public StockRankingResponse getStockRanking(String type, int size) {
        log.debug("Fetching stock ranking from KIS API: type={}, size={}", type, size);

        try {

            if ("volume".equals(type) || "trading_value".equals(type)) {
                return fetchVolumeRanking(type, size);
            }

            List<StockRankingResponse.RankedStock> stocks = fetchRankingByMarket("J", type, Math.max(30, size));

            log.info("Fluctuation ranking: {} items returned for type={}", stocks.size(), type);

            if ("losers".equals(type)) {
                stocks.sort((a, b) -> Double.compare(a.changeRate(), b.changeRate()));
            } else {
                stocks.sort((a, b) -> Double.compare(b.changeRate(), a.changeRate()));
            }

            List<StockRankingResponse.RankedStock> result = reAssignRanks(stocks, size);
            return new StockRankingResponse(type, result, Instant.now());

        } catch (Exception e) {
            log.error("KIS ranking API failed (type={}): {}", type, e.getMessage(), e);
            return new StockRankingResponse(type, List.of(), Instant.now());
        }
    }

    private StockRankingResponse fetchVolumeRanking(String type, int size) {

        String blngClsCode = "trading_value".equals(type) ? "3" : "0";

        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("FID_COND_MRKT_DIV_CODE", "J");
        queryParams.put("FID_COND_SCR_DIV_CODE", "20171");
        queryParams.put("FID_INPUT_ISCD", "0000");
        queryParams.put("FID_DIV_CLS_CODE", "0");
        queryParams.put("FID_BLNG_CLS_CODE", blngClsCode);
        queryParams.put("FID_TRGT_CLS_CODE", "111111111");
        queryParams.put("FID_TRGT_EXLS_CLS_CODE", "0000000000");
        queryParams.put("FID_INPUT_PRICE_1", "");
        queryParams.put("FID_INPUT_PRICE_2", "");
        queryParams.put("FID_VOL_CNT", "");
        queryParams.put("FID_INPUT_DATE_1", "");

        Map<String, Object> response = apiClient.get(VOLUME_RANKING_PATH, TR_ID_VOLUME_RANKING, queryParams);
        List<Map<String, Object>> outputList = getOutputList(response);
        log.debug("KIS volume-rank API returned {} items for type={}", outputList.size(), type);

        List<StockRankingResponse.RankedStock> stocks = new ArrayList<>();
        int count = 0;
        for (Map<String, Object> item : outputList) {
            if (count >= size) break;

            String stockCode = getString(item, "mksc_shrn_iscd");
            String stockName = getString(item, "hts_kor_isnm");
            double currentPrice = getDouble(item, "stck_prpr");
            double changeAmount = getDouble(item, "prdy_vrss");
            double changeRate = getDouble(item, "prdy_ctrt");
            long volume = getLong(item, "acml_vol");
            double tradingValue = (double) getLong(item, "acml_tr_pbmn");

            if (stockCode.isEmpty() || stockName.isEmpty()) continue;
            if (currentPrice == 0) continue;

            stocks.add(new StockRankingResponse.RankedStock(
                    count + 1, stockCode, stockName,
                    currentPrice, changeAmount, changeRate, volume, tradingValue));
            count++;
        }

        return new StockRankingResponse(type, stocks, Instant.now());
    }

    private List<StockRankingResponse.RankedStock> fetchRankingByMarket(
            String marketDivCode, String type, int size) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("FID_COND_MRKT_DIV_CODE", marketDivCode);
        queryParams.put("FID_COND_SCR_DIV_CODE", "20170");
        queryParams.put("FID_INPUT_ISCD", "0000");
        queryParams.put("FID_RANK_SORT_CLS_CODE", getRankSortCode(type));
        queryParams.put("FID_INPUT_CNT_1", "0");
        queryParams.put("FID_PRC_CLS_CODE", "0");
        queryParams.put("FID_INPUT_PRICE_1", "");
        queryParams.put("FID_INPUT_PRICE_2", "");
        queryParams.put("FID_VOL_CNT", "");
        queryParams.put("FID_TRGT_CLS_CODE", "0");
        queryParams.put("FID_TRGT_EXLS_CLS_CODE", "0");
        queryParams.put("FID_DIV_CLS_CODE", "0");
        queryParams.put("FID_RSFL_RATE1", "");
        queryParams.put("FID_RSFL_RATE2", "");

        Map<String, Object> response = apiClient.get(FLUCTUATION_RANKING_PATH, TR_ID_FLUCTUATION_RANKING, queryParams);
        List<Map<String, Object>> outputList = getOutputList(response);
        log.info("KIS fluctuation API (market={}) returned {} items for type={}",
                marketDivCode, outputList.size(), type);

        List<StockRankingResponse.RankedStock> stocks = new ArrayList<>();
        int count = 0;
        for (Map<String, Object> item : outputList) {
            if (count >= size) break;

            String stockCode = getString(item, "stck_shrn_iscd");
            String stockName = getString(item, "hts_kor_isnm");
            double currentPrice = getDouble(item, "stck_prpr");
            double changeAmount = getDouble(item, "prdy_vrss");
            double changeRate = getDouble(item, "prdy_ctrt");
            long volume = getLong(item, "acml_vol");
            double tradingValue = (double) getLong(item, "acml_tr_pbmn");

            if (stockCode.isEmpty() || stockName.isEmpty()) continue;
            if (currentPrice == 0) continue;

            stocks.add(new StockRankingResponse.RankedStock(
                    0, stockCode, stockName,
                    currentPrice, changeAmount, changeRate, volume, tradingValue));
            count++;
        }
        return stocks;
    }

    private List<StockRankingResponse.RankedStock> reAssignRanks(
            List<StockRankingResponse.RankedStock> merged, int size) {
        List<StockRankingResponse.RankedStock> result = new ArrayList<>();
        int limit = Math.min(size, merged.size());
        for (int i = 0; i < limit; i++) {
            var s = merged.get(i);
            result.add(new StockRankingResponse.RankedStock(
                    i + 1, s.stockCode(), s.stockName(),
                    s.currentPrice(), s.changeAmount(), s.changeRate(),
                    s.volume(), s.tradingValue()));
        }
        return result;
    }

    private String getRankSortCode(String type) {
        return switch (type) {
            case "losers" -> "1";
            default -> "0";
        };
    }

    private MarketIndex fetchSingleIndex(IndexConfig config) {

        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("FID_COND_MRKT_DIV_CODE", "U");
        queryParams.put("FID_INPUT_ISCD", config.kisCode());

        Map<String, Object> response = apiClient.get(INQUIRE_INDEX_PRICE_PATH, TR_ID_INDEX_PRICE, queryParams);
        Map<String, Object> output = getOutput(response);

        double currentValue = getDouble(output, "bstp_nmix_prpr");
        double changeAmount = getDouble(output, "bstp_nmix_prdy_vrss");
        double changeRate = getDouble(output, "bstp_nmix_prdy_ctrt");

        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        List<Double> sparklineData = fetchSparklineData(config.kisCode());

        if (currentValue == 0.0 && !sparklineData.isEmpty()) {

            for (int i = sparklineData.size() - 1; i >= 0; i--) {
                if (sparklineData.get(i) != 0.0) {
                    currentValue = sparklineData.get(i);
                    break;
                }
            }
        }

        List<Double> filteredSparkline = sparklineData.stream()
                .filter(v -> v != 0.0)
                .toList();

        return new MarketIndex(
                config.displayCode(),
                config.name(),
                currentValue,
                changeAmount,
                changeRate,
                filteredSparkline,
                Instant.now()
        );
    }

    private List<Double> fetchSparklineData(String kisIndexCode) {
        try {
            Map<String, String> queryParams = new LinkedHashMap<>();
            queryParams.put("FID_COND_MRKT_DIV_CODE", "U");
            queryParams.put("FID_INPUT_ISCD", kisIndexCode);
            queryParams.put("FID_INPUT_DATE_1", formatDateDaysAgo(10));
            queryParams.put("FID_INPUT_DATE_2", formatDateToday());
            queryParams.put("FID_PERIOD_DIV_CODE", "D");

            Map<String, Object> response = apiClient.get(INQUIRE_DAILY_INDEX_PATH, TR_ID_DAILY_INDEX, queryParams);
            List<Map<String, Object>> output2 = getOutput2(response);

            List<Double> values = output2.stream()
                    .limit(5)
                    .map(item -> getDouble(item, "bstp_nmix_prpr"))
                    .toList();

            List<Double> reversed = new ArrayList<>(values);
            Collections.reverse(reversed);
            return reversed;

        } catch (Exception e) {
            log.warn("Failed to fetch sparkline data for index {}: {}", kisIndexCode, e.getMessage());
            return List.of();
        }
    }

    private String formatDateToday() {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String formatDateDaysAgo(int days) {
        return java.time.LocalDate.now().minusDays(days)
                .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    }

    private MarketIndex fetchOverseasIndex(OverseasIndexConfig config) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("FID_COND_MRKT_DIV_CODE", "N");
        queryParams.put("FID_INPUT_ISCD", config.symbol());
        queryParams.put("FID_INPUT_DATE_1", formatDateDaysAgo(15));
        queryParams.put("FID_INPUT_DATE_2", formatDateToday());
        queryParams.put("FID_PERIOD_DIV_CODE", "D");

        Map<String, Object> response = apiClient.get(OVERSEAS_INDEX_PRICE_PATH, TR_ID_OVERSEAS_INDEX, queryParams);

        Map<String, Object> output1 = getOutput1(response);

        List<Map<String, Object>> output2 = getOutput2(response);

        double currentValue = getDouble(output1, "ovrs_nmix_prpr");
        double changeAmount = getDouble(output1, "ovrs_nmix_prdy_vrss");
        double changeRate = getDouble(output1, "prdy_ctrt");

        if (currentValue == 0.0 && !output2.isEmpty()) {
            currentValue = getDouble(output2.get(0), "ovrs_nmix_prpr");
            if (currentValue == 0.0) {
                currentValue = getDouble(output2.get(0), "stck_clpr");
            }

            double prevValue = 0.0;
            if (output2.size() > 1) {
                prevValue = getDouble(output2.get(1), "ovrs_nmix_prpr");
                if (prevValue == 0.0) {
                    prevValue = getDouble(output2.get(1), "stck_clpr");
                }
            }
            changeAmount = prevValue > 0 ? currentValue - prevValue : 0.0;
            changeRate = prevValue > 0 ? (changeAmount / prevValue) * 100 : 0.0;
        }

        if (currentValue == 0.0 && output2.isEmpty()) {
            log.warn("No data returned for overseas index {}", config.displayCode());
            return new MarketIndex(config.displayCode(), config.name(), 0.0, 0.0, 0.0, List.of(), Instant.now());
        }

        List<Double> sparkline = output2.stream()
                .limit(5)
                .map(item -> {
                    double v = getDouble(item, "ovrs_nmix_prpr");
                    return v == 0.0 ? getDouble(item, "stck_clpr") : v;
                })
                .toList();

        List<Double> reversed = new ArrayList<>(sparkline);
        Collections.reverse(reversed);
        List<Double> filtered = reversed.stream().filter(v -> v != 0.0).toList();

        return new MarketIndex(config.displayCode(), config.name(),
                currentValue, changeAmount, changeRate, filtered, Instant.now());
    }

    private static final String OVERSEAS_SEARCH_PATH = "/uapi/overseas-price/v1/quotations/inquire-search";
    private static final String TR_ID_OVERSEAS_SEARCH = "HHDFS76410000";

    @Override
    public StockRankingResponse getOverseasStockRanking(String type, int size) {
        log.debug("Fetching overseas stock ranking from KIS API: type={}, size={}", type, size);

        try {
            List<StockRankingResponse.RankedStock> allStocks = new ArrayList<>();

            for (String exchangeCode : List.of("NAS", "NYS")) {
                try {
                    List<StockRankingResponse.RankedStock> stocks =
                            fetchOverseasRanking(exchangeCode, Math.max(30, size));
                    allStocks.addAll(stocks);
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.warn("Failed to fetch overseas ranking for {}: {}", exchangeCode, e.getMessage());
                }
            }

            if (allStocks.isEmpty()) {
                return new StockRankingResponse(type, List.of(), Instant.now());
            }

            switch (type) {
                case "losers" -> allStocks.sort((a, b) -> Double.compare(a.changeRate(), b.changeRate()));
                case "volume" -> allStocks.sort((a, b) -> Long.compare(b.volume(), a.volume()));
                case "trading_value" -> allStocks.sort((a, b) -> Double.compare(b.tradingValue(), a.tradingValue()));
                default -> allStocks.sort((a, b) -> Double.compare(b.changeRate(), a.changeRate()));
            }

            if ("gainers".equals(type)) {
                allStocks.removeIf(s -> s.changeRate() <= 0);
            } else if ("losers".equals(type)) {
                allStocks.removeIf(s -> s.changeRate() >= 0);
            }

            List<StockRankingResponse.RankedStock> result = reAssignRanks(allStocks, size);
            return new StockRankingResponse(type, result, Instant.now());

        } catch (Exception e) {
            log.error("KIS overseas ranking API failed (type={}): {}", type, e.getMessage(), e);
            return new StockRankingResponse(type, List.of(), Instant.now());
        }
    }

    private List<StockRankingResponse.RankedStock> fetchOverseasRanking(String exchangeCode, int size) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("AUTH", "");
        queryParams.put("EXCD", exchangeCode);
        queryParams.put("CO_YN_PRICECUR", "");
        queryParams.put("CO_ST_PRICECUR", "");
        queryParams.put("CO_EN_PRICECUR", "");
        queryParams.put("CO_YN_RATE", "");
        queryParams.put("CO_ST_RATE", "");
        queryParams.put("CO_EN_RATE", "");
        queryParams.put("CO_YN_VALX", "");
        queryParams.put("CO_ST_VALX", "");
        queryParams.put("CO_EN_VALX", "");
        queryParams.put("CO_YN_SHAR", "");
        queryParams.put("CO_ST_SHAR", "");
        queryParams.put("CO_EN_SHAR", "");
        queryParams.put("CO_YN_VOLUME", "");
        queryParams.put("CO_ST_VOLUME", "");
        queryParams.put("CO_EN_VOLUME", "");
        queryParams.put("CO_YN_AMT", "");
        queryParams.put("CO_ST_AMT", "");
        queryParams.put("CO_EN_AMT", "");
        queryParams.put("CO_YN_EPS", "");
        queryParams.put("CO_ST_EPS", "");
        queryParams.put("CO_EN_EPS", "");
        queryParams.put("CO_YN_PER", "");
        queryParams.put("CO_ST_PER", "");
        queryParams.put("CO_EN_PER", "");

        Map<String, Object> response = apiClient.get(OVERSEAS_SEARCH_PATH, TR_ID_OVERSEAS_SEARCH, queryParams);
        List<Map<String, Object>> output2 = getOutput2(response);
        log.info("KIS overseas search API ({}) returned {} items", exchangeCode, output2.size());

        List<StockRankingResponse.RankedStock> stocks = new ArrayList<>();
        int count = 0;
        for (Map<String, Object> item : output2) {
            if (count >= size) break;

            String stockCode = getString(item, "symb");
            String stockName = getString(item, "name");
            double currentPrice = getDouble(item, "last");
            double changeAmount = getDouble(item, "diff");
            double changeRate = getDouble(item, "rate");
            long volume = getLong(item, "tvol");
            double tradingValue = getDouble(item, "tomv");

            if (stockCode.isEmpty() || stockName.isEmpty()) continue;
            if (currentPrice == 0) continue;

            String sign = getString(item, "sign");
            if ("5".equals(sign) || "4".equals(sign)) {

                changeAmount = -Math.abs(changeAmount);
            }

            if (tradingValue == 0 && volume > 0) {
                tradingValue = currentPrice * volume;
            }

            stocks.add(new StockRankingResponse.RankedStock(
                    0, stockCode, stockName,
                    currentPrice, changeAmount, changeRate, volume, tradingValue));
            count++;
        }
        return stocks;
    }

    private record IndexConfig(String kisCode, String displayCode, String name) {}

    private record OverseasIndexConfig(String exchangeCode, String symbol, String displayCode, String name) {}
}
