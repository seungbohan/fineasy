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
public class KisOverseasDailyPriceSyncService {

    private static final Logger log = LoggerFactory.getLogger(KisOverseasDailyPriceSyncService.class);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final String OVERSEAS_PRICE_PATH = "/uapi/overseas-price/v1/quotations/price";
    private static final String TR_ID_OVERSEAS_PRICE = "HHDFS00000300";

    private static final String OVERSEAS_DAILY_PRICE_PATH = "/uapi/overseas-price/v1/quotations/dailyprice";
    private static final String TR_ID_OVERSEAS_DAILY = "HHDFS76240000";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private static final long API_CALL_DELAY_MS = 200;

    private static final int SAVE_BATCH_SIZE = 100;

    private static final int BACKFILL_DAYS = 365;

    private final KisApiClient apiClient;
    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;

    public KisOverseasDailyPriceSyncService(KisApiClient apiClient,
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
            log.warn("Overseas historical backfill failed on startup: {}", e.getMessage());
        }

        LocalDateTime nowKst = LocalDateTime.now(KST);
        DayOfWeek dow = nowKst.getDayOfWeek();

        if (dow == DayOfWeek.SUNDAY || dow == DayOfWeek.SATURDAY) {
            log.info("Overseas daily price sync skipped on startup (weekend)");
            return;
        }

        try {
            log.info("Overseas daily price sync triggered on startup");
            syncDailyPrices();
        } catch (Exception e) {
            log.warn("Overseas daily price sync failed on startup: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 7 * * MON-FRI", zone = "Asia/Seoul")
    @SchedulerLock(name = "kisOverseasDailyPriceSync", lockAtLeastFor = "PT10M", lockAtMostFor = "PT2H")
    public void scheduledSync() {
        log.info("Scheduled overseas daily price sync started");
        syncDailyPrices();
    }

    @Transactional
    public void backfillIfNeeded() {
        List<StockEntity> overseasStocks = stockRepository.findAllByMarkets(
                List.of(Market.NASDAQ, Market.NYSE));

        if (overseasStocks.isEmpty()) {
            return;
        }

        StockEntity sample = overseasStocks.get(0);
        long priceCount = stockPriceRepository.countByStockId(sample.getId());

        if (priceCount >= 120) {
            log.info("Overseas historical price data sufficient ({} records for {}), skipping backfill",
                    priceCount, sample.getStockCode());
            return;
        }

        log.info("Insufficient overseas price data ({} records for {}), starting historical backfill...",
                priceCount, sample.getStockCode());

        LocalDate to = LocalDate.now(KST);
        LocalDate from = to.minusDays(BACKFILL_DAYS);

        int totalStocks = overseasStocks.size();
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < totalStocks; i++) {
            StockEntity stock = overseasStocks.get(i);

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
                    log.warn("Failed to backfill overseas {} ({}): {}",
                            stock.getStockCode(), stock.getStockName(), e.getMessage());
                }
            }

            if ((i + 1) % 10 == 0 || i == totalStocks - 1) {
                log.info("Overseas backfill progress: {}/{} stocks (success={}, fail={})",
                        i + 1, totalStocks, successCount, failCount);
            }
        }

        log.info("Overseas historical backfill completed: success={}, fail={}, total={}",
                successCount, failCount, totalStocks);
    }

    private int backfillStock(StockEntity stock, LocalDate from, LocalDate to) {
        String exchangeCode = KisExchangeCodeMapper.toExchangeCode(stock.getMarket());
        List<StockPriceEntity> allPrices = new ArrayList<>();
        LocalDate chunkEnd = to;

        while (chunkEnd.isAfter(from)) {
            List<StockPriceEntity> chunk = fetchOverseasDailyData(stock, exchangeCode, chunkEnd);

            for (StockPriceEntity price : chunk) {
                if (!price.getTradeDate().isBefore(from) && !price.getTradeDate().isAfter(to)) {
                    allPrices.add(price);
                }
            }

            if (chunk.isEmpty()) break;

            LocalDate earliest = chunk.stream()
                    .map(StockPriceEntity::getTradeDate)
                    .min(Comparator.naturalOrder())
                    .orElse(from);

            chunkEnd = earliest.minusDays(1);

            if (chunkEnd.isAfter(from)) {
                sleep(API_CALL_DELAY_MS);
            }
        }

        if (!allPrices.isEmpty()) {
            stockPriceRepository.saveAll(allPrices);
        }

        return allPrices.size();
    }

    private List<StockPriceEntity> fetchOverseasDailyData(StockEntity stock,
                                                            String exchangeCode,
                                                            LocalDate baseDate) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("AUTH", "");
        queryParams.put("EXCD", exchangeCode);
        queryParams.put("SYMB", stock.getStockCode());
        queryParams.put("GUBN", "0");
        queryParams.put("BYMD", baseDate.format(DATE_FMT));
        queryParams.put("MODP", "1");

        Map<String, Object> response = apiClient.get(
                OVERSEAS_DAILY_PRICE_PATH, TR_ID_OVERSEAS_DAILY, queryParams);
        List<Map<String, Object>> output2 = getOutput2(response);

        List<StockPriceEntity> prices = new ArrayList<>();
        for (Map<String, Object> item : output2) {
            String dateStr = getString(item, "xymd");
            if (dateStr.isEmpty()) continue;

            BigDecimal closePrice = getBigDecimal(item, "clos");
            if (closePrice.compareTo(BigDecimal.ZERO) == 0) continue;

            LocalDate tradeDate = LocalDate.parse(dateStr, DATE_FMT);
            BigDecimal openPrice = getBigDecimal(item, "open");
            BigDecimal highPrice = getBigDecimal(item, "high");
            BigDecimal lowPrice = getBigDecimal(item, "low");
            long volume = getLong(item, "tvol");

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

        List<StockEntity> overseasStocks = stockRepository.findAllByMarkets(
                List.of(Market.NASDAQ, Market.NYSE));

        if (overseasStocks.isEmpty()) {
            log.warn("No overseas stocks found in DB, skipping price sync");
            return;
        }

        Set<Long> alreadySynced = stockPriceRepository.findStockIdsWithPriceOnDate(today);
        log.info("Overseas daily price sync: {} total stocks, {} already synced for {}",
                overseasStocks.size(), alreadySynced.size(), today);

        List<StockEntity> toSync = overseasStocks.stream()
                .filter(s -> !alreadySynced.contains(s.getId()))
                .toList();

        if (toSync.isEmpty()) {
            log.info("All overseas stocks already synced for {}, nothing to do", today);
            return;
        }

        log.info("Syncing overseas prices for {} stocks...", toSync.size());

        List<StockPriceEntity> batch = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < toSync.size(); i++) {
            StockEntity stock = toSync.get(i);

            try {
                StockPriceEntity price = fetchAndBuildOverseasPrice(stock, today);
                if (price != null) {
                    batch.add(price);
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                if (failCount <= 10) {
                    log.warn("Failed to fetch overseas price for {} ({}): {}",
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

            if ((i + 1) % 50 == 0) {
                log.info("Overseas price sync progress: {}/{} (success={}, fail={})",
                        i + 1, toSync.size(), successCount, failCount);
            }
        }

        if (!batch.isEmpty()) {
            stockPriceRepository.saveAll(batch);
        }

        log.info("Overseas daily price sync completed for {}: success={}, fail={}, total={}",
                today, successCount, failCount, toSync.size());
    }

    private StockPriceEntity fetchAndBuildOverseasPrice(StockEntity stock, LocalDate tradeDate) {
        String exchangeCode = KisExchangeCodeMapper.toExchangeCode(stock.getMarket());

        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("AUTH", "");
        queryParams.put("EXCD", exchangeCode);
        queryParams.put("SYMB", stock.getStockCode());

        Map<String, Object> response = apiClient.get(
                OVERSEAS_PRICE_PATH, TR_ID_OVERSEAS_PRICE, queryParams);
        Map<String, Object> output = getOutput(response);

        BigDecimal closePrice = getBigDecimal(output, "last");
        if (closePrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal openPrice = getBigDecimal(output, "t_xprc");
        BigDecimal highPrice = getBigDecimal(output, "t_hprc");
        BigDecimal lowPrice = getBigDecimal(output, "t_lprc");
        long volume = getLong(output, "tvol");

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
