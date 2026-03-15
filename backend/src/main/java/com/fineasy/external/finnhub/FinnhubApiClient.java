package com.fineasy.external.finnhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.config.FinnhubApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@ConditionalOnExpression("!'${finnhub.api.key:}'.isEmpty()")
@EnableConfigurationProperties(FinnhubApiProperties.class)
public class FinnhubApiClient {

    private static final Logger log = LoggerFactory.getLogger(FinnhubApiClient.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FinnhubApiProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public FinnhubApiClient(FinnhubApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(properties.resolvedBaseUrl())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                .build();

        log.info("Finnhub API client initialized with base URL: {}", properties.resolvedBaseUrl());
    }

    /**
     * Fetch company news from Finnhub for a given symbol and date range.
     * Returns a JSON array of news articles.
     */
    public JsonNode fetchCompanyNews(String symbol, LocalDate from, LocalDate to) {
        try {
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/company-news")
                            .queryParam("symbol", symbol)
                            .queryParam("from", from.format(DATE_FMT))
                            .queryParam("to", to.format(DATE_FMT))
                            .queryParam("token", properties.key())
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(2, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5)))
                    .block(Duration.ofSeconds(15));

            if (body == null || body.isBlank()) {
                log.warn("Finnhub company-news returned empty for symbol={}", symbol);
                return null;
            }

            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("Finnhub company-news API call failed for symbol={}: {}", symbol, e.getMessage());
            return null;
        }
    }
}
