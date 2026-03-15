package com.fineasy.external.kis;

import com.fineasy.entity.Market;
import com.fineasy.entity.StockEntity;
import com.fineasy.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@ConditionalOnProperty(name = "data-provider.type", havingValue = "kis")
public class KisOverseasStockSyncService {

    private static final Logger log = LoggerFactory.getLogger(KisOverseasStockSyncService.class);

    private static final String NAS_URL = "https://new.real.download.dws.co.kr/common/master/nasmst.cod.zip";
    private static final String NYS_URL = "https://new.real.download.dws.co.kr/common/master/nysmst.cod.zip";
    private static final String AMS_URL = "https://new.real.download.dws.co.kr/common/master/amsmst.cod.zip";

    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    // KIS master file format (tab-separated):
    // 0: country(US)  1: ??  2: exchange(NAS/NYS/AMS)  3: country_kr
    // 4: symbol  5: exchange+symbol  6: korean_name  7: english_name
    // 8: stock_type  9: currency  ...
    private static final int FIELD_SYMBOL = 4;
    private static final int FIELD_KOREAN_NAME = 6;
    private static final int FIELD_ENGLISH_NAME = 7;
    private static final int MIN_FIELDS = 8;

    private final StockRepository stockRepository;
    private final HttpClient httpClient;

    public KisOverseasStockSyncService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncOnStartup() {
        log.info("Starting overseas stock master sync...");
        try {
            Map<String, StockEntity> existingByCode = stockRepository.findAll().stream()
                    .collect(Collectors.toMap(StockEntity::getStockCode, s -> s, (a, b) -> a));

            int nasCount = syncMarket(NAS_URL, Market.NASDAQ, existingByCode);
            int nysCount = syncMarket(NYS_URL, Market.NYSE, existingByCode);
            int amsCount = syncMarket(AMS_URL, Market.AMEX, existingByCode);

            log.info("Overseas stock master sync completed. NASDAQ={}, NYSE={}, AMEX={}",
                    nasCount, nysCount, amsCount);
        } catch (Exception e) {
            log.warn("Overseas stock master sync failed (non-fatal): {}", e.getMessage());
        }
    }

    private int syncMarket(String url, Market market, Map<String, StockEntity> existingByCode) {
        try {
            byte[] zipBytes = downloadFile(url);
            String content = extractFromZip(zipBytes);
            List<StockRecord> records = parseMasterFile(content);

            List<StockEntity> newStocks = new ArrayList<>();
            int updated = 0;
            LocalDateTime now = LocalDateTime.now();

            for (StockRecord record : records) {
                StockEntity existing = existingByCode.get(record.code);
                if (existing == null) {
                    StockEntity stock = new StockEntity(
                            null, record.code, record.displayName(),
                            market, null, true, now
                    );
                    newStocks.add(stock);
                    existingByCode.put(record.code, stock);
                } else if (!existing.getStockName().equals(record.displayName())) {
                    existing.updateName(record.displayName());
                    updated++;
                }
            }

            if (!newStocks.isEmpty()) {
                stockRepository.saveAll(newStocks);
            }

            log.info("Parsed {} {} stocks, {} new, {} name-updated",
                    records.size(), market, newStocks.size(), updated);
            return newStocks.size();
        } catch (Exception e) {
            log.warn("Failed to sync {} overseas stocks: {}", market, e.getMessage());
            return 0;
        }
    }

    private byte[] downloadFile(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException("Download failed with status " + response.statusCode());
        }
        return response.body();
    }

    private String extractFromZip(byte[] zipBytes) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry != null) {
                return new String(zis.readAllBytes(), EUC_KR);
            }
        }
        throw new IOException("Empty zip file");
    }

    private List<StockRecord> parseMasterFile(String content) {
        List<StockRecord> records = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String line : lines) {
            if (line.isBlank()) continue;

            String[] parts = line.split("\t");
            if (parts.length < MIN_FIELDS) continue;

            String symbol = parts[FIELD_SYMBOL].trim();
            String koreanName = parts[FIELD_KOREAN_NAME].trim();
            String englishName = parts[FIELD_ENGLISH_NAME].trim();

            // Skip invalid entries
            if (symbol.isEmpty() || symbol.length() > 10) continue;
            // Skip symbols with spaces (header or invalid)
            if (symbol.contains(" ")) continue;

            records.add(new StockRecord(symbol, koreanName, englishName));
        }
        return records;
    }

    private record StockRecord(String code, String koreanName, String englishName) {
        String displayName() {
            if (koreanName.isEmpty()) return englishName;
            return koreanName;
        }
    }
}
