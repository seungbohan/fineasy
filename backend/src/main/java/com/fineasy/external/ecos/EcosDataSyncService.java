package com.fineasy.external.ecos;

import com.fineasy.entity.MacroIndicatorEntity;
import com.fineasy.repository.MacroIndicatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@ConditionalOnBean(EcosApiClient.class)
public class EcosDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(EcosDataSyncService.class);
    private static final DateTimeFormatter DAILY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTHLY_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    private final EcosApiClient ecosApiClient;
    private final MacroIndicatorRepository macroRepo;

    public EcosDataSyncService(EcosApiClient ecosApiClient,
                                MacroIndicatorRepository macroRepo) {
        this.ecosApiClient = ecosApiClient;
        this.macroRepo = macroRepo;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void initialSync() {

        java.time.LocalDate threeDaysAgo = java.time.LocalDate.now().minusDays(3);
        boolean hasRecentData = macroRepo.existsByIndicatorCodeAndRecordDateAfter(
                EcosIndicatorDef.values()[0].code(), threeDaysAgo);
        if (hasRecentData) {
            log.info("ECOS data already up-to-date, skipping initial sync.");
            return;
        }

        log.info("Starting initial ECOS data sync...");
        syncAllIndicators(90);
        log.info("Initial ECOS data sync completed.");
    }

    @Scheduled(fixedRate = 6 * 60 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    @SchedulerLock(name = "ecosSyncDaily", lockAtLeastFor = "PT10M", lockAtMostFor = "PT5H")
    public void periodicSyncDaily() {
        log.info("Starting periodic ECOS daily indicator sync...");
        syncIndicatorsByFrequency(7, true);
        log.info("Periodic ECOS daily indicator sync completed.");
    }

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000, initialDelay = 10 * 60 * 1000)
    @SchedulerLock(name = "ecosSyncMonthlyQuarterly", lockAtLeastFor = "PT30M", lockAtMostFor = "PT23H")
    public void periodicSyncMonthlyQuarterly() {
        log.info("Starting periodic ECOS monthly/quarterly indicator sync...");
        syncIndicatorsByFrequency(30, false);
        log.info("Periodic ECOS monthly/quarterly indicator sync completed.");
    }

    @Transactional
    public void syncAllIndicators(int lookbackDays) {
        for (EcosIndicatorDef def : EcosIndicatorDef.values()) {
            try {
                syncIndicator(def, lookbackDays);
            } catch (Exception e) {
                log.error("Failed to sync ECOS indicator {}: {}", def.code(), e.getMessage());
            }
        }
    }

    @Transactional
    public void syncIndicatorsByFrequency(int lookbackDays, boolean dailyOnly) {
        List<EcosIndicatorDef> targets = Arrays.stream(EcosIndicatorDef.values())
                .filter(def -> dailyOnly == def.isDaily())
                .toList();

        for (EcosIndicatorDef def : targets) {
            try {
                syncIndicator(def, lookbackDays);
            } catch (Exception e) {
                log.error("Failed to sync ECOS indicator {}: {}", def.code(), e.getMessage());
            }
        }
    }

    private void syncIndicator(EcosIndicatorDef def, int lookbackDays) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(lookbackDays);

        String start = formatDateForApi(def, startDate);
        String end = formatDateForApi(def, endDate);
        int maxRows = calculateFetchLimit(def, lookbackDays);

        List<EcosApiClient.EcosDataRow> rows = ecosApiClient.fetchStatistic(
                def.statCode(), def.itemCode1(), def.cycle(), start, end, maxRows);

        if (rows.isEmpty()) {
            log.debug("No data returned from ECOS for {} (statCode={})", def.code(), def.statCode());
            return;
        }

        int saved = 0;
        for (EcosApiClient.EcosDataRow row : rows) {
            Double value = row.valueAsDouble();
            if (value == null) continue;

            LocalDate recordDate = parseTime(row.time(), def);
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
            log.info("Synced {} new data points for {} ({}, statCode={})",
                    saved, def.code(), def.displayName(), def.statCode());
        }
    }

    private String formatDateForApi(EcosIndicatorDef def, LocalDate date) {
        if (def.isDaily()) {
            return date.format(DAILY_FMT);
        } else if (def.isMonthly()) {
            return date.format(MONTHLY_FMT);
        } else {

            int quarter = (date.getMonthValue() - 1) / 3 + 1;
            return date.getYear() + "Q" + quarter;
        }
    }

    private LocalDate parseTime(String time, EcosIndicatorDef def) {
        try {
            if (def.isDaily()) {
                return LocalDate.parse(time, DAILY_FMT);
            } else if (def.isMonthly()) {
                return LocalDate.parse(time + "01", DAILY_FMT);
            } else {

                return parseQuarterlyDate(time);
            }
        } catch (Exception e) {
            log.warn("Failed to parse ECOS date '{}' for {}: {}", time, def.code(), e.getMessage());
            return null;
        }
    }

    private LocalDate parseQuarterlyDate(String time) {

        int qIndex = time.indexOf('Q');
        if (qIndex < 0) {
            log.warn("Unexpected quarterly date format: '{}'", time);
            return null;
        }

        int year = Integer.parseInt(time.substring(0, qIndex));
        int quarter = Integer.parseInt(time.substring(qIndex + 1));

        int month = (quarter - 1) * 3 + 1;
        return LocalDate.of(year, month, 1);
    }

    private int calculateFetchLimit(EcosIndicatorDef def, int lookbackDays) {
        if (def.isDaily()) {

            return Math.max(50, (int) (lookbackDays * 0.7));
        } else if (def.isMonthly()) {
            return Math.max(12, lookbackDays / 30 + 3);
        } else {

            return Math.max(8, lookbackDays / 90 + 2);
        }
    }
}
