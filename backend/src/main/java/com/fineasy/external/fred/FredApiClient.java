package com.fineasy.external.fred;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.config.FredApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@EnableConfigurationProperties(FredApiProperties.class)
@ConditionalOnExpression("!'${fred.api.key:}'.isEmpty()")
public class FredApiClient {

    private static final Logger log = LoggerFactory.getLogger(FredApiClient.class);

    private final FredApiProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public FredApiClient(FredApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(properties.resolvedBaseUrl())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        log.info("FRED API client initialized (baseUrl={})", properties.resolvedBaseUrl());
    }

    public List<FredDataRow> fetchObservations(String seriesId, int limit) {
        String uri = String.format(
                "/series/observations?series_id=%s&api_key=%s&file_type=json&sort_order=desc&limit=%d",
                seriesId, properties.key(), limit
        );

        try {
            String body = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(2, java.time.Duration.ofSeconds(1))
                            .maxBackoff(java.time.Duration.ofSeconds(5)))
                    .block(java.time.Duration.ofSeconds(30));

            return parseResponse(body, seriesId);
        } catch (Exception e) {
            log.error("FRED API call failed for seriesId={}: {}", seriesId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<FredDataRow> parseResponse(String body, String seriesId) {
        try {
            JsonNode root = objectMapper.readTree(body);

            JsonNode errorCode = root.path("error_code");
            if (!errorCode.isMissingNode()) {
                String errorMessage = root.path("error_message").asText("Unknown error");
                log.warn("FRED API returned error for {}: {} - {}", seriesId,
                        errorCode.asText(), errorMessage);
                return Collections.emptyList();
            }

            JsonNode observations = root.path("observations");
            if (observations.isMissingNode() || !observations.isArray()) {
                log.debug("No observations in FRED response for {}", seriesId);
                return Collections.emptyList();
            }

            List<FredDataRow> rows = new ArrayList<>();
            for (JsonNode obs : observations) {
                String date = obs.path("date").asText();
                String value = obs.path("value").asText();

                if (".".equals(value) || value.isBlank()) {
                    continue;
                }

                rows.add(new FredDataRow(date, value));
            }
            return rows;
        } catch (Exception e) {
            log.error("Failed to parse FRED API response for {}: {}", seriesId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public record FredDataRow(String date, String value) {

        public Double valueAsDouble() {
            try {
                return Double.parseDouble(value.replace(",", ""));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
