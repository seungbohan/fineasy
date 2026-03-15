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
import java.nio.charset.StandardCharsets;
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

    private final StockRepository stockRepository;
    private final HttpClient httpClient;

    public KisOverseasStockSyncService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private static final Map<String, String> KOREAN_NAMES = Map.ofEntries(
            Map.entry("AAPL", "애플"), Map.entry("MSFT", "마이크로소프트"),
            Map.entry("NVDA", "엔비디아"), Map.entry("GOOGL", "알파벳(구글)"),
            Map.entry("GOOG", "알파벳(구글)"), Map.entry("AMZN", "아마존"),
            Map.entry("META", "메타(페이스북)"), Map.entry("TSLA", "테슬라"),
            Map.entry("AVGO", "브로드컴"), Map.entry("COST", "코스트코"),
            Map.entry("NFLX", "넷플릭스"), Map.entry("AMD", "AMD"),
            Map.entry("ADBE", "어도비"), Map.entry("QCOM", "퀄컴"),
            Map.entry("INTC", "인텔"), Map.entry("CSCO", "시스코"),
            Map.entry("PEP", "펩시코"), Map.entry("TMUS", "T모바일"),
            Map.entry("AMGN", "암젠"), Map.entry("HON", "허니웰"),
            Map.entry("MU", "마이크론"), Map.entry("PYPL", "페이팔"),
            Map.entry("COIN", "코인베이스"), Map.entry("PLTR", "팔란티어"),
            Map.entry("BRK/B", "버크셔해서웨이"), Map.entry("BRK.B", "버크셔해서웨이"),
            Map.entry("LLY", "일라이릴리"), Map.entry("JPM", "JP모건"),
            Map.entry("V", "비자"), Map.entry("UNH", "유나이티드헬스"),
            Map.entry("MA", "마스터카드"), Map.entry("XOM", "엑슨모빌"),
            Map.entry("JNJ", "존슨앤존슨"), Map.entry("PG", "P&G"),
            Map.entry("HD", "홈디포"), Map.entry("WMT", "월마트"),
            Map.entry("CVX", "셰브론"), Map.entry("BAC", "뱅크오브아메리카"),
            Map.entry("KO", "코카콜라"), Map.entry("MRK", "머크"),
            Map.entry("ABBV", "애브비"), Map.entry("CRM", "세일즈포스"),
            Map.entry("DIS", "디즈니"), Map.entry("NKE", "나이키"),
            Map.entry("BA", "보잉"), Map.entry("GS", "골드만삭스"),
            Map.entry("CAT", "캐터필러"), Map.entry("IBM", "IBM"),
            Map.entry("GE", "GE에어로스페이스"), Map.entry("T", "AT&T"),
            Map.entry("VZ", "버라이즌"), Map.entry("UBER", "우버"),
            Map.entry("ARM", "ARM홀딩스"), Map.entry("TSM", "TSMC"),
            Map.entry("ASML", "ASML"), Map.entry("CRWD", "크라우드스트라이크"),
            Map.entry("PANW", "팔로알토네트웍스"), Map.entry("SNPS", "시놉시스"),
            Map.entry("LRCX", "램리서치"), Map.entry("AMAT", "어플라이드머티리얼즈"),
            Map.entry("MRVL", "마벨테크놀로지"), Map.entry("NOW", "서비스나우"),
            Map.entry("SMCI", "슈퍼마이크로"), Map.entry("BKNG", "부킹홀딩스"),
            Map.entry("MELI", "메르카도리브레"), Map.entry("INTU", "인튜이트"),
            Map.entry("TXN", "텍사스인스트루먼트"), Map.entry("KLAC", "KLA"),
            Map.entry("CMCSA", "컴캐스트"), Map.entry("ISRG", "인튜이티브서지컬"),
            Map.entry("NEE", "넥스트에라에너지"), Map.entry("UPS", "UPS"),
            Map.entry("MMM", "3M")
    );

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncOnStartup() {
        log.info("Starting overseas stock master sync...");
        try {
            Set<String> existingCodes = stockRepository.findAll().stream()
                    .map(StockEntity::getStockCode)
                    .collect(Collectors.toSet());

            int nasCount = syncMarket(NAS_URL, "nasmst.cod", Market.NASDAQ, existingCodes);
            int nysCount = syncMarket(NYS_URL, "nysmst.cod", Market.NYSE, existingCodes);
            int amsCount = syncMarket(AMS_URL, "amsmst.cod", Market.AMEX, existingCodes);

            log.info("Overseas stock master sync completed. NASDAQ={}, NYSE={}, AMEX={}",
                    nasCount, nysCount, amsCount);
        } catch (Exception e) {
            log.warn("Overseas stock master sync failed (non-fatal): {}", e.getMessage());
        }
    }

    private int syncMarket(String url, String entryName, Market market, Set<String> existingCodes) {
        try {
            byte[] zipBytes = downloadFile(url);
            String content = extractFromZip(zipBytes, entryName);
            List<StockRecord> records = parseMasterFile(content);

            List<StockEntity> newStocks = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (StockRecord record : records) {
                if (!existingCodes.contains(record.code)) {
                    String name = buildDisplayName(record.code, record.name);
                    newStocks.add(new StockEntity(
                            null, record.code, name,
                            market, record.sector, true, now
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
            log.warn("Failed to sync {} overseas stocks: {}", market, e.getMessage());
            return 0;
        }
    }

    private String buildDisplayName(String code, String engName) {
        String kr = KOREAN_NAMES.get(code);
        return kr != null ? kr + " " + engName : engName;
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

    private String extractFromZip(byte[] zipBytes, String entryName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equalsIgnoreCase(entryName)) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        // If exact name not found, try first entry
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry != null) {
                return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        throw new IOException("No entry found in zip for: " + entryName);
    }

    /**
     * KIS overseas master file format (tab-separated):
     * Each line contains: symbol, exchange, name, sector info, etc.
     * Format may vary, so we parse flexibly.
     */
    private List<StockRecord> parseMasterFile(String content) {
        List<StockRecord> records = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String line : lines) {
            if (line.isBlank()) continue;

            String[] parts = line.split("\t");
            if (parts.length < 2) continue;

            // First field is typically the symbol, second is the name
            String symbol = parts[0].trim();

            // Skip empty symbols, headers, or invalid entries
            if (symbol.isEmpty() || symbol.contains(" ") || symbol.length() > 10) continue;

            String name = parts.length > 1 ? parts[1].trim() : symbol;
            String sector = parts.length > 2 ? parts[2].trim() : null;

            // Skip if name looks like a header
            if (name.equalsIgnoreCase("NAME") || symbol.equalsIgnoreCase("SYMBOL")) continue;

            if (sector != null && sector.length() > 100) {
                sector = sector.substring(0, 100);
            }

            records.add(new StockRecord(symbol, name, sector));
        }
        return records;
    }

    private record StockRecord(String code, String name, String sector) {}
}
