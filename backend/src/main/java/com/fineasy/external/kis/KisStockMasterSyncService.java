package com.fineasy.external.kis;

import com.fineasy.entity.Market;
import com.fineasy.entity.StockEntity;
import com.fineasy.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@ConditionalOnProperty(name = "data-provider.type", havingValue = "kis")
public class KisStockMasterSyncService {

    private static final Logger log = LoggerFactory.getLogger(KisStockMasterSyncService.class);

    private static final String KOSPI_URL = "https://new.real.download.dws.co.kr/common/master/kospi_code.mst.zip";
    private static final String KOSDAQ_URL = "https://new.real.download.dws.co.kr/common/master/kosdaq_code.mst.zip";

    private static final Charset CP949 = Charset.forName("EUC-KR");
    private static final int TAIL_FIELD_LENGTH = 228;

    private final StockRepository stockRepository;
    private final HttpClient httpClient;

    public KisStockMasterSyncService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        log.info("Starting KIS stock master sync...");
        try {
            Set<String> existingCodes = stockRepository.findAll().stream()
                    .map(StockEntity::getStockCode)
                    .collect(Collectors.toSet());

            int kospiCount = syncMarket(KOSPI_URL, "kospi_code.mst", Market.KRX, existingCodes);
            int kosdaqCount = syncMarket(KOSDAQ_URL, "kosdaq_code.mst", Market.KOSDAQ, existingCodes);

            log.info("KIS stock master sync completed. New stocks added: KOSPI={}, KOSDAQ={}", kospiCount, kosdaqCount);
        } catch (Exception e) {
            log.warn("KIS stock master sync failed (non-fatal): {}", e.getMessage());
        }
    }

    private int syncMarket(String url, String entryName, Market market, Set<String> existingCodes) {
        try {
            byte[] zipBytes = downloadFile(url);
            String mstContent = extractMstFromZip(zipBytes, entryName);
            List<StockRecord> records = parseMstFile(mstContent);

            List<StockEntity> newStocks = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (StockRecord record : records) {
                if (!existingCodes.contains(record.code)) {
                    newStocks.add(new StockEntity(
                            null, record.code, record.name,
                            market, null, true, now
                    ));
                    existingCodes.add(record.code);
                }
            }

            if (!newStocks.isEmpty()) {
                stockRepository.saveAll(newStocks);
            }

            log.info("Parsed {} {} stocks, {} new", records.size(), market, newStocks.size());
            return newStocks.size();
        } catch (Exception e) {
            log.warn("Failed to sync {} stocks: {}", market, e.getMessage());
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

    private String extractMstFromZip(byte[] zipBytes, String entryName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    return new String(zis.readAllBytes(), CP949);
                }
            }
        }
        throw new IOException("Entry not found in zip: " + entryName);
    }

    private List<StockRecord> parseMstFile(String content) {
        List<StockRecord> records = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String line : lines) {
            if (line.length() < TAIL_FIELD_LENGTH + 22) continue;

            String shortCode = line.substring(0, 9).trim();

            if (shortCode.length() != 6 || !shortCode.matches("\\d{6}")) continue;

            String rest = line.substring(21);
            String name = rest.substring(0, rest.length() - TAIL_FIELD_LENGTH).trim();

            if (name.isEmpty()) continue;

            records.add(new StockRecord(shortCode, name));
        }
        return records;
    }

    private record StockRecord(String code, String name) {}
}
