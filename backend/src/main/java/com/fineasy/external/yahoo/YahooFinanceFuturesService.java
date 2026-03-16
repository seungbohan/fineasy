package com.fineasy.external.yahoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.entity.MacroIndicatorEntity;
import com.fineasy.repository.MacroIndicatorRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class YahooFinanceFuturesService {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceFuturesService.class);

    private static final String YAHOO_QUOTE_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?range=1d&interval=1d";

    private record FuturesDef(String indicatorCode, String yahooSymbol, String displayName,
                              String unit) {}

    private static final FuturesDef[] FUTURES_DEFS = {
            new FuturesDef("GOLD", "GC=F", "금 선물", "달러/oz"),
            new FuturesDef("SILVER", "SI=F", "은 선물", "달러/oz"),
            new FuturesDef("WTI", "CL=F", "WTI 원유 선물", "달러/배럴"),
            new FuturesDef("US_DXY", "DX-Y.NYB", "달러 인덱스", "Index"),
    };

    private final MacroIndicatorRepository macroRepo;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public YahooFinanceFuturesService(MacroIndicatorRepository macroRepo,
                                       ObjectMapper objectMapper) {
        this.macroRepo = macroRepo;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void initialSync() {
        log.info("Starting initial Yahoo Finance futures sync...");
        syncAllFutures();
        log.info("Initial Yahoo Finance futures sync completed.");
    }

    @Scheduled(fixedRate = 6 * 60 * 60 * 1000, initialDelay = 3 * 60 * 1000)
    @SchedulerLock(name = "yahooFuturesSync", lockAtLeastFor = "PT5M", lockAtMostFor = "PT1H")
    public void periodicSync() {
        log.info("Starting periodic Yahoo Finance futures sync...");
        syncAllFutures();
        log.info("Periodic Yahoo Finance futures sync completed.");
    }

    public void syncAllFutures() {
        for (FuturesDef def : FUTURES_DEFS) {
            try {
                syncFutures(def);
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to sync Yahoo futures {}: {}", def.indicatorCode(), e.getMessage());
            }
        }
    }

    private void syncFutures(FuturesDef def) {
        String url = String.format(YAHOO_QUOTE_URL, def.yahooSymbol());

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Yahoo Finance returned {} for {}", response.statusCode(), def.indicatorCode());
                return;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode meta = root.path("chart").path("result").path(0).path("meta");

            double price = meta.path("regularMarketPrice").asDouble(0.0);
            if (price == 0.0) {
                log.warn("Zero price from Yahoo Finance for {}", def.indicatorCode());
                return;
            }

            LocalDate today = LocalDate.now();

            Optional<MacroIndicatorEntity> existing = macroRepo.findLatestByCode(def.indicatorCode());
            Long existingId = existing.map(MacroIndicatorEntity::getId).orElse(null);

            macroRepo.save(new MacroIndicatorEntity(
                    existingId, def.indicatorCode(), def.displayName(),
                    price, def.unit(), today, "Yahoo Finance"));
            log.info("Saved Yahoo futures {} = {}", def.indicatorCode(), price);

        } catch (Exception e) {
            log.error("Yahoo Finance API error for {}: {}", def.indicatorCode(), e.getMessage());
        }
    }
}
