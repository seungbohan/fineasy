package com.fineasy.external.sec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fineasy.entity.Market;
import com.fineasy.entity.StockEntity;
import com.fineasy.entity.StockPriceEntity;
import com.fineasy.repository.StockPriceRepository;
import com.fineasy.repository.StockRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Syncs accurate market cap for overseas stocks using SEC EDGAR Company Facts API.
 * Market cap = shares outstanding (from SEC) × latest close price (from DB).
 * Runs on startup (if needed) and weekly.
 */
@Service
public class SecMarketCapSyncService {

    private static final Logger log = LoggerFactory.getLogger(SecMarketCapSyncService.class);

    private static final long API_CALL_DELAY_MS = 200; // SEC allows 10 req/sec

    private final SecEdgarApiClient secEdgarApiClient;
    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;

    public SecMarketCapSyncService(SecEdgarApiClient secEdgarApiClient,
                                    StockRepository stockRepository,
                                    StockPriceRepository stockPriceRepository) {
        this.secEdgarApiClient = secEdgarApiClient;
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        // Ensure CIK mapping is loaded
        secEdgarApiClient.loadCompanyTickers();

        List<StockEntity> overseasStocks = stockRepository.findAllByMarkets(
                List.of(Market.NASDAQ, Market.NYSE, Market.AMEX));

        long withMarketCap = overseasStocks.stream()
                .filter(s -> s.getMarketCapUsd() != null && s.getMarketCapUsd().compareTo(BigDecimal.ZERO) > 0)
                .count();

        if (withMarketCap > overseasStocks.size() * 0.8) {
            log.info("Market cap data already populated for {}% of overseas stocks, skipping startup sync.",
                    overseasStocks.isEmpty() ? 0 : (withMarketCap * 100) / overseasStocks.size());
            return;
        }

        log.info("Starting SEC market cap sync for {} overseas stocks ({} already have data)...",
                overseasStocks.size(), withMarketCap);
        syncMarketCaps(overseasStocks);
    }

    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Seoul")
    @SchedulerLock(name = "secMarketCapSync", lockAtLeastFor = "PT10M", lockAtMostFor = "PT2H")
    public void weeklySync() {
        log.info("Starting weekly SEC market cap sync...");
        List<StockEntity> overseasStocks = stockRepository.findAllByMarkets(
                List.of(Market.NASDAQ, Market.NYSE, Market.AMEX));
        syncMarketCaps(overseasStocks);
        log.info("Weekly SEC market cap sync completed.");
    }

    @Transactional
    public void syncMarketCaps(List<StockEntity> stocks) {
        if (stocks.isEmpty()) {
            log.info("No overseas stocks found for market cap sync.");
            return;
        }

        int success = 0;
        int fail = 0;
        int noCik = 0;

        for (int i = 0; i < stocks.size(); i++) {
            StockEntity stock = stocks.get(i);

            try {
                BigDecimal marketCap = calculateMarketCap(stock);
                if (marketCap != null && marketCap.compareTo(BigDecimal.ZERO) > 0) {
                    stock.updateMarketCapUsd(marketCap);
                    stockRepository.save(stock);
                    success++;
                } else {
                    fail++;
                }
            } catch (Exception e) {
                fail++;
                if (fail <= 5) {
                    log.warn("Failed to sync market cap for {}: {}", stock.getStockCode(), e.getMessage());
                }
            }

            if (i < stocks.size() - 1) {
                sleep(API_CALL_DELAY_MS);
            }

            if ((i + 1) % 100 == 0) {
                log.info("SEC market cap sync progress: {}/{} (success={}, fail={}, noCik={})",
                        i + 1, stocks.size(), success, fail, noCik);
            }
        }

        log.info("SEC market cap sync finished: success={}, fail={}, noCik={}, total={}",
                success, fail, noCik, stocks.size());
    }

    private BigDecimal calculateMarketCap(StockEntity stock) {
        // Step 1: Resolve CIK
        String cik = secEdgarApiClient.resolveCik(stock.getStockCode());
        if (cik == null) {
            log.debug("No CIK found for {}", stock.getStockCode());
            return null;
        }

        // Step 2: Fetch company facts and extract shares outstanding
        JsonNode companyFacts = secEdgarApiClient.fetchCompanyFacts(cik);
        Long sharesOutstanding = secEdgarApiClient.extractSharesOutstanding(companyFacts);
        if (sharesOutstanding == null) {
            log.debug("No shares outstanding data for {} (CIK={})", stock.getStockCode(), cik);
            return null;
        }

        // Step 3: Get latest close price from DB
        Optional<StockPriceEntity> latestPrice = stockPriceRepository.findLatestByStockId(stock.getId());
        if (latestPrice.isEmpty() || latestPrice.get().getClosePrice() == null) {
            log.debug("No price data for {}", stock.getStockCode());
            return null;
        }

        BigDecimal closePrice = latestPrice.get().getClosePrice();

        // Step 4: Market cap = shares outstanding × close price
        return closePrice.multiply(BigDecimal.valueOf(sharesOutstanding));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
