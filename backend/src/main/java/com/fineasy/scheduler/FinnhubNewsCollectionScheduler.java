package com.fineasy.scheduler;

import com.fineasy.service.FinnhubNewsCollectorService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnExpression("!'${finnhub.api.key:}'.isEmpty()")
public class FinnhubNewsCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(FinnhubNewsCollectionScheduler.class);

    private final FinnhubNewsCollectorService finnhubNewsCollectorService;

    public FinnhubNewsCollectionScheduler(FinnhubNewsCollectorService finnhubNewsCollectorService) {
        this.finnhubNewsCollectorService = finnhubNewsCollectorService;
    }

    /**
     * Collect overseas stock news from Finnhub every 30 minutes.
     */
    @Scheduled(fixedRate = 1800000)
    @SchedulerLock(name = "collectFinnhubNews", lockAtLeastFor = "PT5M", lockAtMostFor = "PT25M")
    public void collectFinnhubNews() {
        log.info("Starting scheduled Finnhub news collection...");

        try {
            List<String> symbols = finnhubNewsCollectorService.getOverseasStockSymbols();
            if (symbols.isEmpty()) {
                log.info("No overseas stock symbols found for Finnhub news collection.");
                return;
            }

            log.info("Collecting Finnhub news for {} overseas symbols", symbols.size());
            int totalSaved = finnhubNewsCollectorService.collectNewsForSymbols(symbols);
            log.info("Finnhub news collection completed. Total new articles saved: {}", totalSaved);
        } catch (Exception e) {
            log.error("Finnhub news collection failed: {}", e.getMessage(), e);
        }
    }
}
