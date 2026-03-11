package com.fineasy.external.fred;

import com.fineasy.entity.MacroIndicatorEntity;
import com.fineasy.repository.MacroIndicatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@ConditionalOnBean(FredApiClient.class)
public class FredDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(FredDataSyncService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FredApiClient fredApiClient;
    private final MacroIndicatorRepository macroRepo;

    public FredDataSyncService(FredApiClient fredApiClient,
                                MacroIndicatorRepository macroRepo) {
        this.fredApiClient = fredApiClient;
        this.macroRepo = macroRepo;
    }

    @PostConstruct
    public void initialSync() {
        log.info("Starting initial FRED data sync...");
        syncAllIndicators(90);
        log.info("Initial FRED data sync completed.");
    }

    @Scheduled(fixedRate = 6 * 60 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    @SchedulerLock(name = "fredSyncDaily", lockAtLeastFor = "PT10M", lockAtMostFor = "PT5H")
    public void periodicSyncDaily() {
        log.info("Starting periodic FRED daily indicator sync...");
        syncIndicatorsByFrequency(7, true);
        log.info("Periodic FRED daily indicator sync completed.");
    }

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000, initialDelay = 10 * 60 * 1000)
    @SchedulerLock(name = "fredSyncMonthlyQuarterly", lockAtLeastFor = "PT30M", lockAtMostFor = "PT23H")
    public void periodicSyncMonthlyQuarterly() {
        log.info("Starting periodic FRED monthly/quarterly indicator sync...");
        syncIndicatorsByFrequency(30, false);
        log.info("Periodic FRED monthly/quarterly indicator sync completed.");
    }

    @Transactional
    public void syncAllIndicators(int lookbackDays) {
        for (FredIndicatorDef def : FredIndicatorDef.values()) {
            try {
                syncIndicator(def, lookbackDays);
            } catch (Exception e) {
                log.error("Failed to sync FRED indicator {}: {}", def.code(), e.getMessage());
            }
        }
    }

    @Transactional
    public void syncIndicatorsByFrequency(int lookbackDays, boolean dailyOnly) {
        List<FredIndicatorDef> targets = Arrays.stream(FredIndicatorDef.values())
                .filter(def -> dailyOnly == def.isDaily())
                .toList();

        for (FredIndicatorDef def : targets) {
            try {
                syncIndicator(def, lookbackDays);
            } catch (Exception e) {
                log.error("Failed to sync FRED indicator {}: {}", def.code(), e.getMessage());
            }
        }
    }

    private void syncIndicator(FredIndicatorDef def, int lookbackDays) {
        int limit = calculateFetchLimit(def, lookbackDays);

        List<FredApiClient.FredDataRow> rows = fredApiClient.fetchObservations(
                def.seriesId(), limit);

        if (rows.isEmpty()) {
            log.debug("No data returned from FRED for {} (seriesId={})", def.code(), def.seriesId());
            return;
        }

        int saved = 0;
        for (FredApiClient.FredDataRow row : rows) {
            Double value = row.valueAsDouble();
            if (value == null) continue;

            LocalDate recordDate = parseDate(row.date());
            if (recordDate == null) continue;

            if (macroRepo.existsByIndicatorCodeAndRecordDate(def.code(), recordDate)) {
                continue;
            }

            macroRepo.save(new MacroIndicatorEntity(
                    null,
                    def.code(),
                    def.displayName(),
                    value,
                    def.unit(),
                    recordDate,
                    def.source()
            ));
            saved++;
        }

        if (saved > 0) {
            log.info("Synced {} new data points for {} ({}, seriesId={})",
                    saved, def.code(), def.displayName(), def.seriesId());
        }
    }

    private int calculateFetchLimit(FredIndicatorDef def, int lookbackDays) {
        if (def.isDaily()) {
            return Math.max(50, (int) (lookbackDays * 0.7));
        } else if (def.isMonthly()) {
            return Math.max(12, lookbackDays / 30 + 3);
        } else {
            return Math.max(8, lookbackDays / 90 + 2);
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FMT);
        } catch (Exception e) {
            log.warn("Failed to parse FRED date '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }
}
