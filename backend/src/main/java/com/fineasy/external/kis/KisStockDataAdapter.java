package com.fineasy.external.kis;

import com.fineasy.dto.response.StockChartResponse;
import com.fineasy.dto.response.StockFinancialsResponse;
import com.fineasy.dto.response.StockPriceResponse;
import com.fineasy.entity.Market;
import com.fineasy.entity.StockEntity;
import com.fineasy.repository.StockRepository;
import com.fineasy.service.StockDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.fineasy.external.kis.KisResponseParser.*;

@Component
@Primary
@ConditionalOnProperty(name = "data-provider.type", havingValue = "kis")
public class KisStockDataAdapter implements StockDataProvider {

    private static final Logger log = LoggerFactory.getLogger(KisStockDataAdapter.class);

    private static final String INQUIRE_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String DAILY_CHART_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
    private static final String MINUTE_CHART_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice";

    private static final String TR_ID_INQUIRE_PRICE = "FHKST01010100";
    private static final String TR_ID_DAILY_CHART = "FHKST03010100";
    private static final String TR_ID_MINUTE_CHART = "FHKST03010200";

    private static final String MARKET_DIV_CODE_KRX = "J";

    private static final String OVERSEAS_PRICE_PATH = "/uapi/overseas-price/v1/quotations/price";
    private static final String OVERSEAS_DAILY_PRICE_PATH = "/uapi/overseas-price/v1/quotations/dailyprice";

    private static final String TR_ID_OVERSEAS_PRICE = "HHDFS00000300";
    private static final String TR_ID_OVERSEAS_DAILY = "HHDFS76240000";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final KisApiClient apiClient;
    private final StockRepository stockRepository;

    public KisStockDataAdapter(KisApiClient apiClient, StockRepository stockRepository) {
        this.apiClient = apiClient;
        this.stockRepository = stockRepository;
    }

    @Override
    public BigDecimal getCurrentPrice(String stockCode) {
        log.debug("Fetching current price for stock: {}", stockCode);

        if (isOverseas(stockCode)) {
            return getOverseasCurrentPrice(stockCode);
        }
        return getDomesticCurrentPrice(stockCode);
    }

    @Override
    public StockPriceResponse getStockPriceDetail(String stockCode, String stockName) {
        log.debug("Fetching stock price detail for stock: {}", stockCode);

        if (isOverseas(stockCode)) {
            return getOverseasPriceDetail(stockCode, stockName);
        }
        return getDomesticPriceDetail(stockCode, stockName);
    }

    @Override
    public List<StockChartResponse.CandleData> getDailyPrices(String stockCode, LocalDate from, LocalDate to) {
        log.debug("Fetching daily prices for stock: {} from {} to {}", stockCode, from, to);

        if (isOverseas(stockCode)) {
            return getOverseasDailyPrices(stockCode, from, to);
        }
        return getDomesticDailyPrices(stockCode, from, to);
    }

    @Override
    public List<StockChartResponse.CandleData> getMinutePrices(String stockCode) {
        log.debug("Fetching minute prices for stock: {}", stockCode);

        if (isOverseas(stockCode)) {

            log.debug("Minute prices not supported for overseas stock: {}", stockCode);
            return List.of();
        }
        return getDomesticMinutePrices(stockCode);
    }

    @Override
    public StockFinancialsResponse getFinancials(String stockCode) {
        log.debug("Fetching financials for stock: {}", stockCode);

        if (isOverseas(stockCode)) {
            return getOverseasFinancials(stockCode);
        }
        return getDomesticFinancials(stockCode);
    }

    private boolean isOverseas(String stockCode) {
        Optional<StockEntity> stock = stockRepository.findByStockCode(stockCode);
        if (stock.isPresent()) {
            return KisExchangeCodeMapper.isOverseasMarket(stock.get().getMarket());
        }

        return KisExchangeCodeMapper.isOverseasStockCode(stockCode);
    }

    private String resolveExchangeCode(String stockCode) {
        Optional<StockEntity> stock = stockRepository.findByStockCode(stockCode);
        if (stock.isPresent()) {
            return KisExchangeCodeMapper.toExchangeCode(stock.get().getMarket());
        }

        return "NAS";
    }

    private BigDecimal getDomesticCurrentPrice(String stockCode) {
        Map<String, Object> response = callDomesticInquirePrice(stockCode);
        Map<String, Object> output = getOutput(response);
        return getBigDecimal(output, "stck_prpr");
    }

    private StockPriceResponse getDomesticPriceDetail(String stockCode, String stockName) {
        Map<String, Object> response = callDomesticInquirePrice(stockCode);
        Map<String, Object> output = getOutput(response);

        BigDecimal currentPrice = getBigDecimal(output, "stck_prpr");
        BigDecimal changeAmount = getBigDecimal(output, "prdy_vrss");
        double changeRate = getDouble(output, "prdy_ctrt");
        long volume = getLong(output, "acml_vol");

        return new StockPriceResponse(
                stockCode, stockName, currentPrice,
                changeAmount, changeRate, volume, LocalDate.now()
        );
    }

    private List<StockChartResponse.CandleData> getDomesticDailyPrices(String stockCode, LocalDate from, LocalDate to) {
        List<StockChartResponse.CandleData> allCandles = new ArrayList<>();
        LocalDate chunkEnd = to;

        while (chunkEnd.isAfter(from)) {
            LocalDate chunkStart = chunkEnd.minusMonths(3);
            if (chunkStart.isBefore(from)) chunkStart = from;

            List<StockChartResponse.CandleData> chunk = fetchDomesticDailyChunk(stockCode, chunkStart, chunkEnd);
            allCandles.addAll(chunk);

            chunkEnd = chunkStart.minusDays(1);

            if (chunkEnd.isAfter(from)) {
                sleep(100);
            }
        }

        allCandles.sort(Comparator.comparing(StockChartResponse.CandleData::date));
        return allCandles;
    }

    private List<StockChartResponse.CandleData> fetchDomesticDailyChunk(String stockCode, LocalDate from, LocalDate to) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("FID_COND_MRKT_DIV_CODE", MARKET_DIV_CODE_KRX);
        queryParams.put("FID_INPUT_ISCD", stockCode);
        queryParams.put("FID_INPUT_DATE_1", from.format(DATE_FMT));
        queryParams.put("FID_INPUT_DATE_2", to.format(DATE_FMT));
        queryParams.put("FID_PERIOD_DIV_CODE", "D");
        queryParams.put("FID_ORG_ADJ_PRC", "0");

        Map<String, Object> response = apiClient.get(DAILY_CHART_PRICE_PATH, TR_ID_DAILY_CHART, queryParams);
        List<Map<String, Object>> output2 = getOutput2(response);

        List<StockChartResponse.CandleData> candles = new ArrayList<>();
        for (Map<String, Object> item : output2) {
            String dateStr = getString(item, "stck_bsop_date");
            if (dateStr.isEmpty()) continue;

            LocalDate date = LocalDate.parse(dateStr, DATE_FMT);
            BigDecimal open = getBigDecimal(item, "stck_oprc");
            BigDecimal high = getBigDecimal(item, "stck_hgpr");
            BigDecimal low = getBigDecimal(item, "stck_lwpr");
            BigDecimal close = getBigDecimal(item, "stck_clpr");
            long volume = getLong(item, "acml_vol");

            if (close.compareTo(BigDecimal.ZERO) == 0) continue;

            candles.add(new StockChartResponse.CandleData(date.toString(), open, high, low, close, volume));
        }
        return candles;
    }

    private List<StockChartResponse.CandleData> getDomesticMinutePrices(String stockCode) {
        Set<String> seen = new HashSet<>();
        List<StockChartResponse.CandleData> allCandles = new ArrayList<>();
        String cursorTime = "160000";
        String targetDate = null;

        for (int page = 0; page < 15; page++) {
            List<Map<String, Object>> output2 = fetchDomesticMinuteChunk(stockCode, cursorTime);
            if (output2.isEmpty()) break;

            String earliestTime = null;
            boolean hitPreviousDay = false;

            for (Map<String, Object> item : output2) {
                String timeStr = getString(item, "stck_cntg_hour");
                String dateStr = getString(item, "stck_bsop_date");
                if (timeStr.isEmpty() || dateStr.isEmpty()) continue;

                if (targetDate == null) {
                    targetDate = dateStr;
                }

                if (!dateStr.equals(targetDate)) {
                    hitPreviousDay = true;
                    continue;
                }

                String key = dateStr + timeStr;
                if (!seen.add(key)) continue;

                LocalDate date = LocalDate.parse(dateStr, DATE_FMT);
                LocalTime time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HHmmss"));

                String datetime = date + " " + String.format("%02d:%02d", time.getHour(), time.getMinute());

                BigDecimal open = getBigDecimal(item, "stck_oprc");
                BigDecimal high = getBigDecimal(item, "stck_hgpr");
                BigDecimal low = getBigDecimal(item, "stck_lwpr");
                BigDecimal close = getBigDecimal(item, "stck_prpr");
                long volume = getLong(item, "cntg_vol");

                if (close.compareTo(BigDecimal.ZERO) == 0) continue;

                allCandles.add(new StockChartResponse.CandleData(datetime, open, high, low, close, volume));

                if (earliestTime == null || timeStr.compareTo(earliestTime) < 0) {
                    earliestTime = timeStr;
                }
            }

            if (hitPreviousDay || earliestTime == null || earliestTime.compareTo("090000") <= 0) break;

            cursorTime = earliestTime;
            sleep(100);
        }

        allCandles.sort(Comparator.comparing(StockChartResponse.CandleData::date));
        return allCandles;
    }

    private List<Map<String, Object>> fetchDomesticMinuteChunk(String stockCode, String hourCursor) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("FID_COND_MRKT_DIV_CODE", MARKET_DIV_CODE_KRX);
        queryParams.put("FID_INPUT_ISCD", stockCode);
        queryParams.put("FID_INPUT_HOUR_1", hourCursor);
        queryParams.put("FID_ETC_CLS_CODE", "");
        queryParams.put("FID_PW_DATA_INCU_YN", "Y");

        Map<String, Object> response = apiClient.get(MINUTE_CHART_PRICE_PATH, TR_ID_MINUTE_CHART, queryParams);
        return getOutput2(response);
    }

    private StockFinancialsResponse getDomesticFinancials(String stockCode) {
        Map<String, Object> response = callDomesticInquirePrice(stockCode);
        Map<String, Object> output = getOutput(response);
        return mapDomesticFinancialsResponse(stockCode, output);
    }

    private Map<String, Object> callDomesticInquirePrice(String stockCode) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("FID_COND_MRKT_DIV_CODE", MARKET_DIV_CODE_KRX);
        queryParams.put("FID_INPUT_ISCD", stockCode);

        return apiClient.get(INQUIRE_PRICE_PATH, TR_ID_INQUIRE_PRICE, queryParams);
    }

    private StockFinancialsResponse mapDomesticFinancialsResponse(String stockCode, Map<String, Object> output) {
        String stockName = getString(output, "hts_kor_isnm");
        if (stockName.isEmpty()) {
            stockName = getString(output, "rprs_mrkt_kor_name");
        }

        BigDecimal marketCapUnit = getBigDecimal(output, "hts_avls");
        BigDecimal marketCap = marketCapUnit.multiply(new BigDecimal("100000000"));

        long sharesOutstanding = getLong(output, "lstn_stcn");

        Double per = parseNullableDouble(output, "per");
        Double pbr = parseNullableDouble(output, "pbr");
        Double eps = parseNullableDouble(output, "eps");
        Double dividendYield = parseNullableDouble(output, "hts_divi_rate");

        BigDecimal high52Week = getBigDecimal(output, "stck_dryy_hgpr");
        BigDecimal low52Week = getBigDecimal(output, "stck_dryy_lwpr");

        return new StockFinancialsResponse(
                stockCode, stockName, marketCap, sharesOutstanding,
                per, pbr, eps, dividendYield, high52Week, low52Week
        );
    }

    private BigDecimal getOverseasCurrentPrice(String stockCode) {
        Map<String, Object> output = callOverseasPrice(stockCode);
        return getBigDecimal(output, "last");
    }

    private StockPriceResponse getOverseasPriceDetail(String stockCode, String stockName) {
        Map<String, Object> output = callOverseasPrice(stockCode);

        BigDecimal currentPrice = getBigDecimal(output, "last");
        BigDecimal changeAmount = getBigDecimal(output, "diff");
        double changeRate = getDouble(output, "rate");
        long volume = getLong(output, "tvol");

        return new StockPriceResponse(
                stockCode, stockName, currentPrice,
                changeAmount, changeRate, volume, LocalDate.now()
        );
    }

    private Map<String, Object> callOverseasPrice(String stockCode) {
        String exchangeCode = resolveExchangeCode(stockCode);

        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("AUTH", "");
        queryParams.put("EXCD", exchangeCode);
        queryParams.put("SYMB", stockCode);

        Map<String, Object> response = apiClient.get(OVERSEAS_PRICE_PATH, TR_ID_OVERSEAS_PRICE, queryParams);
        return getOutput(response);
    }

    private List<StockChartResponse.CandleData> getOverseasDailyPrices(String stockCode, LocalDate from, LocalDate to) {
        String exchangeCode = resolveExchangeCode(stockCode);

        List<StockChartResponse.CandleData> allCandles = new ArrayList<>();
        LocalDate chunkEnd = to;

        while (chunkEnd.isAfter(from)) {
            List<StockChartResponse.CandleData> chunk =
                    fetchOverseasDailyChunk(stockCode, exchangeCode, chunkEnd);

            for (StockChartResponse.CandleData candle : chunk) {
                LocalDate candleDate = LocalDate.parse(candle.date());
                if (!candleDate.isBefore(from) && !candleDate.isAfter(to)) {
                    allCandles.add(candle);
                }
            }

            if (chunk.isEmpty()) break;

            LocalDate earliest = chunk.stream()
                    .map(c -> LocalDate.parse(c.date()))
                    .min(Comparator.naturalOrder())
                    .orElse(from);

            chunkEnd = earliest.minusDays(1);

            if (chunkEnd.isAfter(from)) {
                sleep(100);
            }
        }

        allCandles.sort(Comparator.comparing(StockChartResponse.CandleData::date));
        return allCandles;
    }

    private List<StockChartResponse.CandleData> fetchOverseasDailyChunk(
            String stockCode, String exchangeCode, LocalDate baseDate) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("AUTH", "");
        queryParams.put("EXCD", exchangeCode);
        queryParams.put("SYMB", stockCode);
        queryParams.put("GUBN", "0");
        queryParams.put("BYMD", baseDate.format(DATE_FMT));
        queryParams.put("MODP", "1");

        Map<String, Object> response = apiClient.get(
                OVERSEAS_DAILY_PRICE_PATH, TR_ID_OVERSEAS_DAILY, queryParams);
        List<Map<String, Object>> output2 = getOutput2(response);

        List<StockChartResponse.CandleData> candles = new ArrayList<>();
        for (Map<String, Object> item : output2) {
            String dateStr = getString(item, "xymd");
            if (dateStr.isEmpty()) continue;

            LocalDate date = LocalDate.parse(dateStr, DATE_FMT);
            BigDecimal open = getBigDecimal(item, "open");
            BigDecimal high = getBigDecimal(item, "high");
            BigDecimal low = getBigDecimal(item, "low");
            BigDecimal close = getBigDecimal(item, "clos");
            long volume = getLong(item, "tvol");

            if (close.compareTo(BigDecimal.ZERO) == 0) continue;

            candles.add(new StockChartResponse.CandleData(date.toString(), open, high, low, close, volume));
        }
        return candles;
    }

    private StockFinancialsResponse getOverseasFinancials(String stockCode) {
        Map<String, Object> output = callOverseasPrice(stockCode);

        String stockName = getString(output, "name");
        if (stockName.isEmpty()) {
            stockName = stockCode;
        }

        BigDecimal marketCap = BigDecimal.ZERO;
        long sharesOutstanding = 0L;

        Double per = parseNullableDouble(output, "perx");
        Double pbr = parseNullableDouble(output, "pbrx");
        Double eps = parseNullableDouble(output, "epsx");
        Double dividendYield = null;

        BigDecimal high52Week = getBigDecimal(output, "h52p");
        BigDecimal low52Week = getBigDecimal(output, "l52p");

        return new StockFinancialsResponse(
                stockCode, stockName, marketCap, sharesOutstanding,
                per, pbr, eps, dividendYield, high52Week, low52Week
        );
    }

    private Double parseNullableDouble(Map<String, Object> map, String key) {
        double value = getDouble(map, key);
        return value == 0.0 ? null : value;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
