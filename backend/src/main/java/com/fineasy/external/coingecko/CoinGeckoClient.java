package com.fineasy.external.coingecko;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class CoinGeckoClient {

    private static final Logger log = LoggerFactory.getLogger(CoinGeckoClient.class);
    private static final String BASE_URL = "https://api.coingecko.com/api/v3";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public CoinGeckoClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        log.info("CoinGecko API client initialized (baseUrl={})", BASE_URL);
    }

    public List<CoinMarketData> fetchAllCoinPrices() {
        String ids = CoinDef.allCoingeckoIds();
        String uri = String.format(
                "/simple/price?ids=%s&vs_currencies=usd,krw"
                        + "&include_market_cap=true&include_24hr_vol=true"
                        + "&include_24hr_change=true",
                ids
        );

        try {
            String body = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(2, java.time.Duration.ofSeconds(1))
                            .maxBackoff(java.time.Duration.ofSeconds(5)))
                    .block(java.time.Duration.ofSeconds(30));

            return parseSimplePriceResponse(body);
        } catch (Exception e) {
            log.error("CoinGecko API call failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<CoinMarketData> parseSimplePriceResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            List<CoinMarketData> results = new ArrayList<>();

            for (CoinDef coin : CoinDef.values()) {
                JsonNode coinNode = root.path(coin.coingeckoId());
                if (coinNode.isMissingNode() || coinNode.isEmpty()) {
                    log.debug("No data found for {} in CoinGecko response", coin.coingeckoId());
                    continue;
                }

                results.add(new CoinMarketData(
                        coin.symbol(),
                        coin.displayName(),
                        toBigDecimal(coinNode, "usd"),
                        toBigDecimal(coinNode, "krw"),
                        toBigDecimal(coinNode, "usd_market_cap"),
                        toBigDecimal(coinNode, "usd_24h_vol"),
                        toDouble(coinNode, "usd_24h_change")
                ));
            }

            log.info("Parsed {} coin prices from CoinGecko", results.size());
            return results;
        } catch (Exception e) {
            log.error("Failed to parse CoinGecko response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private BigDecimal toBigDecimal(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(field.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double toDouble(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            return null;
        }
        return field.asDouble();
    }

    public record CoinMarketData(
            String symbol,
            String name,
            BigDecimal priceUsd,
            BigDecimal priceKrw,
            BigDecimal marketCapUsd,
            BigDecimal volume24hUsd,
            Double change24h
    ) {}
}
