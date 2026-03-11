package com.fineasy.external.kis;

import com.fineasy.entity.Market;
import com.fineasy.entity.StockEntity;
import com.fineasy.entity.StockPriceEntity;
import com.fineasy.repository.StockPriceRepository;
import com.fineasy.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.fineasy.external.kis.KisResponseParser.*;

@Service
@ConditionalOnProperty(name = "data-provider.type", havingValue = "kis")
public class KisDailyPriceSyncService {

    private static final Logger log = LoggerFactory.getLogger(KisDailyPriceSyncService.class);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final String INQUIRE_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String TR_ID_INQUIRE_PRICE = "FHKST01010100";

    private static final String DAILY_CHART_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
    private static final String TR_ID_DAILY_CHART = "FHKST03010100";

    private static final String MARKET_DIV_CODE_KRX = "J";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private static final long API_CALL_DELAY_MS = 100;

    private static final int SAVE_BATCH_SIZE = 100;

    private static final int BACKFILL_DAYS = 365;

    private final KisApiClient apiClient;
    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;

    public KisDailyPriceSyncService(KisApiClient apiClient,
                                     StockRepository stockRepository,
                                     StockPriceRepository stockPriceRepository) {
        this.apiClient = apiClient;
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        try {
            backfillIfNeeded();
        } catch (Exception e) {
            log.warn("Historical backfill failed on startup: {}", e.getMessage());
        }

        LocalDateTime nowKst = LocalDateTime.now(KST);
        DayOfWeek dow = nowKst.getDayOfWeek();

        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            log.info("Daily price sync skipped on startup (weekend)");
            return;
        }

        if (nowKst.getHour() < 15 || (nowKst.getHour() == 15 && nowKst.getMinute() < 30)) {
            log.info("Daily price sync skipped on startup (market still open or pre-market)");
            return;
        }

        try {
            log.info("Daily price sync triggered on startup (after market close)");
            syncDailyPrices();
        } catch (Exception e) {
            log.warn("Daily price sync failed on startup: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Seoul")
    @SchedulerLock(name = "kisDailyPriceSync", lockAtLeastFor = "PT10M", lockAtMostFor = "PT2H")
    public void scheduledSync() {
        log.info("Scheduled daily price sync started");
        syncDailyPrices();
    }

    @Transactional
    public void backfillIfNeeded() {
        List<StockEntity> featuredStocks = stockRepository.findAllByMarkets(
                List.of(Market.KRX, Market.KOSDAQ));

        if (featuredStocks.isEmpty()) {
            return;
        }

        StockEntity sample = featuredStocks.get(0);
        long priceCount = stockPriceRepository.countByStockId(sample.getId());

        if (priceCount >= 120) {
            log.info("Historical price data sufficient ({} records for {}), skipping backfill",
                    priceCount, sample.getStockCode());
            return;
        }

        log.info("Insufficient price data ({} records for {}), starting historical backfill...",
                priceCount, sample.getStockCode());

        stockPriceRepository.deleteAll();
        log.info("Cleared existing price data (mock seed)");

        LocalDate to = LocalDate.now(KST);
        LocalDate from = to.minusDays(BACKFILL_DAYS);

        int totalStocks = featuredStocks.size();
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < totalStocks; i++) {
            StockEntity stock = featuredStocks.get(i);

            try {
                int saved = backfillStock(stock, from, to);
                if (saved > 0) {
                    successCount++;
                    log.debug("Backfilled {} days for {} ({})",
                            saved, stock.getStockCode(), stock.getStockName());
                }
            } catch (Exception e) {
                failCount++;
                if (failCount <= 10) {
                    log.warn("Failed to backfill {} ({}): {}",
                            stock.getStockCode(), stock.getStockName(), e.getMessage());
                }
            }

            if ((i + 1) % 10 == 0 || i == totalStocks - 1) {
                log.info("Backfill progress: {}/{} stocks (success={}, fail={})",
                        i + 1, totalStocks, successCount, failCount);
            }
        }

        log.info("Historical backfill completed: success={}, fail={}, total={}",
                successCount, failCount, totalStocks);
    }

    private int backfillStock(StockEntity stock, LocalDate from, LocalDate to) {
        List<StockPriceEntity> allPrices = new ArrayList<>();
        LocalDate chunkEnd = to;

        while (chunkEnd.isAfter(from)) {
            LocalDate chunkStart = chunkEnd.minusMonths(3);
            if (chunkStart.isBefore(from)) {
                chunkStart = from;
            }

            List<StockPriceEntity> chunk = fetchDailyChartData(stock, chunkStart, chunkEnd);
            allPrices.addAll(chunk);

            chunkEnd = chunkStart.minusDays(1);

            if (chunkEnd.isAfter(from)) {
                sleep(API_CALL_DELAY_MS);
            }
        }

        if (!allPrices.isEmpty()) {
            stockPriceRepository.saveAll(allPrices);
        }

        return allPrices.size();
    }

    private List<StockPriceEntity> fetchDailyChartData(StockEntity stock,
                                                        LocalDate from, LocalDate to) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("FID_COND_MRKT_DIV_CODE", MARKET_DIV_CODE_KRX);
        queryParams.put("FID_INPUT_ISCD", stock.getStockCode());
        queryParams.put("FID_INPUT_DATE_1", from.format(DATE_FMT));
        queryParams.put("FID_INPUT_DATE_2", to.format(DATE_FMT));
        queryParams.put("FID_PERIOD_DIV_CODE", "D");
        queryParams.put("FID_ORG_ADJ_PRC", "0");

        Map<String, Object> response = apiClient.get(
                DAILY_CHART_PRICE_PATH, TR_ID_DAILY_CHART, queryParams);
        List<Map<String, Object>> output2 = getOutput2(response);

        List<StockPriceEntity> prices = new ArrayList<>();
        for (Map<String, Object> item : output2) {
            String dateStr = getString(item, "stck_bsop_date");
            if (dateStr.isEmpty()) continue;

            BigDecimal closePrice = getBigDecimal(item, "stck_clpr");
            if (closePrice.compareTo(BigDecimal.ZERO) == 0) continue;

            LocalDate tradeDate = LocalDate.parse(dateStr, DATE_FMT);
            BigDecimal openPrice = getBigDecimal(item, "stck_oprc");
            BigDecimal highPrice = getBigDecimal(item, "stck_hgpr");
            BigDecimal lowPrice = getBigDecimal(item, "stck_lwpr");
            long volume = getLong(item, "acml_vol");

            prices.add(new StockPriceEntity(
                    null, stock, tradeDate,
                    openPrice, highPrice, lowPrice, closePrice,
                    volume, LocalDateTime.now()
            ));
        }
        return prices;
    }

    @Transactional
    public void syncDailyPrices() {
        LocalDate today = LocalDate.now(KST);

        List<StockEntity> domesticStocks = stockRepository.findAllByMarkets(
                List.of(Market.KRX, Market.KOSDAQ));

        if (domesticStocks.isEmpty()) {
            log.warn("No domestic stocks found in DB, skipping price sync");
            return;
        }

        Set<Long> alreadySynced = stockPriceRepository.findStockIdsWithPriceOnDate(today);
        log.info("Daily price sync: {} total domestic stocks, {} already synced for {}",
                domesticStocks.size(), alreadySynced.size(), today);

        List<StockEntity> toSync = domesticStocks.stream()
                .filter(s -> !alreadySynced.contains(s.getId()))
                .toList();

        if (toSync.isEmpty()) {
            log.info("All stocks already synced for {}, nothing to do", today);
            return;
        }

        log.info("Syncing prices for {} stocks...", toSync.size());

        List<StockPriceEntity> batch = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < toSync.size(); i++) {
            StockEntity stock = toSync.get(i);

            try {
                StockPriceEntity price = fetchAndBuildPrice(stock, today);
                if (price != null) {
                    batch.add(price);
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                if (failCount <= 10) {
                    log.warn("Failed to fetch price for {} ({}): {}",
                            stock.getStockCode(), stock.getStockName(), e.getMessage());
                }
            }

            if (batch.size() >= SAVE_BATCH_SIZE) {
                stockPriceRepository.saveAll(batch);
                batch.clear();
            }

            if (i < toSync.size() - 1) {
                sleep(API_CALL_DELAY_MS);
            }

            if ((i + 1) % 500 == 0) {
                log.info("Price sync progress: {}/{} (success={}, fail={})",
                        i + 1, toSync.size(), successCount, failCount);
            }
        }

        if (!batch.isEmpty()) {
            stockPriceRepository.saveAll(batch);
        }

        log.info("Daily price sync completed for {}: success={}, fail={}, total={}",
                today, successCount, failCount, toSync.size());
    }

    private StockPriceEntity fetchAndBuildPrice(StockEntity stock, LocalDate tradeDate) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("FID_COND_MRKT_DIV_CODE", MARKET_DIV_CODE_KRX);
        queryParams.put("FID_INPUT_ISCD", stock.getStockCode());

        Map<String, Object> response = apiClient.get(INQUIRE_PRICE_PATH, TR_ID_INQUIRE_PRICE, queryParams);
        Map<String, Object> output = getOutput(response);

        BigDecimal closePrice = getBigDecimal(output, "stck_prpr");
        if (closePrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal openPrice = getBigDecimal(output, "stck_oprc");
        BigDecimal highPrice = getBigDecimal(output, "stck_hgpr");
        BigDecimal lowPrice = getBigDecimal(output, "stck_lwpr");
        long volume = getLong(output, "acml_vol");

        return new StockPriceEntity(
                null, stock, tradeDate,
                openPrice, highPrice, lowPrice, closePrice,
                volume, LocalDateTime.now()
        );
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
