package com.fineasy.external.sec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.config.SecEdgarApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableConfigurationProperties(SecEdgarApiProperties.class)
public class SecEdgarApiClient {

    private static final Logger log = LoggerFactory.getLogger(SecEdgarApiClient.class);

    private static final String COMPANY_TICKERS_URL = "https://www.sec.gov/files/company_tickers.json";

    private final SecEdgarApiProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // In-memory CIK mapping cache: ticker -> CIK (zero-padded 10 digits)
    private final Map<String, String> tickerToCikMap = new ConcurrentHashMap<>();
    private volatile boolean cikMapLoaded = false;

    public SecEdgarApiClient(SecEdgarApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, properties.resolvedUserAgent())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        log.info("SEC EDGAR API client initialized with User-Agent: {}", properties.resolvedUserAgent());
    }

    /**
     * Load company_tickers.json from SEC into memory for ticker->CIK lookup.
     * Called lazily on first CIK lookup or eagerly on startup.
     */
    public void loadCompanyTickers() {
        if (cikMapLoaded) return;

        try {
            String body = webClient.get()
                    .uri(COMPANY_TICKERS_URL)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(2, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5)))
                    .block(Duration.ofSeconds(30));

            if (body == null || body.isBlank()) {
                log.warn("SEC company_tickers.json returned empty response");
                return;
            }

            JsonNode root = objectMapper.readTree(body);
            root.fields().forEachRemaining(entry -> {
                JsonNode item = entry.getValue();
                String ticker = item.path("ticker").asText("").toUpperCase();
                long cikLong = item.path("cik_str").asLong(0);
                if (!ticker.isBlank() && cikLong > 0) {
                    String cik = String.format("%010d", cikLong);
                    tickerToCikMap.put(ticker, cik);
                }
            });

            cikMapLoaded = true;
            log.info("SEC company_tickers loaded: {} tickers mapped", tickerToCikMap.size());
        } catch (Exception e) {
            log.error("Failed to load SEC company_tickers.json: {}", e.getMessage());
        }
    }

    /**
     * Resolve a stock ticker to a zero-padded CIK number.
     */
    public String resolveCik(String ticker) {
        if (!cikMapLoaded) {
            loadCompanyTickers();
        }
        return tickerToCikMap.get(ticker.toUpperCase());
    }

    /**
     * Fetch recent SEC filings for a given CIK from submissions API.
     */
    public JsonNode fetchFilings(String cik) {
        try {
            String url = properties.resolvedBaseUrl() + "/submissions/CIK" + cik + ".json";

            String body = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(2, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5)))
                    .block(Duration.ofSeconds(30));

            if (body == null || body.isBlank()) {
                log.warn("SEC EDGAR submissions returned empty response for CIK={}", cik);
                return null;
            }

            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("SEC EDGAR submissions API call failed for CIK={}: {}", cik, e.getMessage());
            return null;
        }
    }
}
