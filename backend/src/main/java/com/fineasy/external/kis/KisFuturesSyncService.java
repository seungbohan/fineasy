package com.fineasy.external.kis;

import com.fineasy.entity.MacroIndicatorEntity;
import com.fineasy.repository.MacroIndicatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.fineasy.external.kis.KisResponseParser.*;

@Service
@ConditionalOnProperty(name = "data-provider.type", havingValue = "kis")
public class KisFuturesSyncService {

    private static final Logger log = LoggerFactory.getLogger(KisFuturesSyncService.class);

    private static final String FUTURES_PRICE_PATH =
            "/uapi/overseas-futureoption/v1/quotations/inquire-price";
    private static final String TR_ID_FUTURES_PRICE = "HHDFC55010000";

    private static final char[] MONTH_CODES =
            {' ', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'Q', 'U', 'V', 'X', 'Z'};

    private record FuturesDef(String indicatorCode, String rootSymbol, String displayName,
                              String unit, String category) {}

    private static final FuturesDef[] FUTURES_DEFS = {
            new FuturesDef("GOLD", "GC", "금 선물", "달러/oz", "COMMODITY"),
            new FuturesDef("SILVER", "SI", "은 선물", "달러/oz", "COMMODITY"),
            new FuturesDef("WTI", "CL", "WTI 원유 선물", "달러/배럴", "COMMODITY"),
            new FuturesDef("US_DXY", "DX", "달러 인덱스", "Index", "FINANCIAL_MARKET"),
    };

    private final KisApiClient apiClient;
    private final MacroIndicatorRepository macroRepo;

    public KisFuturesSyncService(KisApiClient apiClient, MacroIndicatorRepository macroRepo) {
        this.apiClient = apiClient;
        this.macroRepo = macroRepo;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialSync() {
        log.info("Starting initial KIS futures price sync...");
        syncAllFutures();
        log.info("Initial KIS futures price sync completed.");
    }

    @Scheduled(fixedRate = 6 * 60 * 60 * 1000, initialDelay = 3 * 60 * 1000)
    @SchedulerLock(name = "kisFuturesSync", lockAtLeastFor = "PT5M", lockAtMostFor = "PT1H")
    public void periodicSync() {
        log.info("Starting periodic KIS futures price sync...");
        syncAllFutures();
        log.info("Periodic KIS futures price sync completed.");
    }

    @Transactional
    public void syncAllFutures() {
        for (FuturesDef def : FUTURES_DEFS) {
            try {
                syncFutures(def);
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to sync KIS futures {}: {}", def.indicatorCode(), e.getMessage());
            }
        }
    }

    private void syncFutures(FuturesDef def) {
        String symbolCode = buildFrontMonthCode(def.rootSymbol());
        log.debug("Fetching KIS futures price: {} -> {}", def.indicatorCode(), symbolCode);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("SRS_CD", symbolCode);

        Map<String, Object> response = apiClient.get(FUTURES_PRICE_PATH, TR_ID_FUTURES_PRICE, params);
        Map<String, Object> output = getOutput1(response);

        if (output.isEmpty()) {
            log.warn("Empty response for futures {}: {}", def.indicatorCode(), symbolCode);
            return;
        }

        double rawPrice = getDouble(output, "last");
        if (rawPrice == 0.0) rawPrice = getDouble(output, "stck_prpr");
        if (rawPrice == 0.0) rawPrice = getDouble(output, "ovrs_nmix_prpr");

        if (rawPrice == 0.0) {
            log.warn("Zero price for futures {}: {}", def.indicatorCode(), symbolCode);
            return;
        }

        final double price = rawPrice;
        LocalDate today = LocalDate.now();

        long deleted = macroRepo.deleteByIndicatorCode(def.indicatorCode());
        if (deleted > 0) {
            log.info("Replaced {} old records for {} with KIS futures data", deleted, def.indicatorCode());
        }

        macroRepo.save(new MacroIndicatorEntity(
                null, def.indicatorCode(), def.displayName(),
                price, def.unit(), today, "KIS"));
        log.info("Saved KIS futures {} = {} ({})", def.indicatorCode(), price, symbolCode);
    }

    private String buildFrontMonthCode(String rootSymbol) {
        LocalDate today = LocalDate.now();

        YearMonth ym = YearMonth.from(today);
        if (today.getDayOfMonth() > 15) {
            ym = ym.plusMonths(1);
        }

        if ("DX".equals(rootSymbol)) {
            int month = ym.getMonthValue();

            int[] quarterlyMonths = {3, 6, 9, 12};
            for (int qm : quarterlyMonths) {
                if (qm >= month) {
                    ym = YearMonth.of(ym.getYear(), qm);
                    break;
                }
            }

            if (ym.getMonthValue() < month) {
                ym = YearMonth.of(ym.getYear() + 1, 3);
            }
        }

        char monthCode = MONTH_CODES[ym.getMonthValue()];
        String yearSuffix = String.valueOf(ym.getYear() % 100);

        return rootSymbol + monthCode + yearSuffix;
    }
}
