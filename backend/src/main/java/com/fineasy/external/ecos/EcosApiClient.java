package com.fineasy.external.ecos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.config.EcosApiProperties;
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
@EnableConfigurationProperties(EcosApiProperties.class)
@ConditionalOnExpression("!'${ecos.api.key:}'.isEmpty()")
public class EcosApiClient {

    private static final Logger log = LoggerFactory.getLogger(EcosApiClient.class);

    private final EcosApiProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EcosApiClient(EcosApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(properties.resolvedBaseUrl())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        log.info("ECOS API client initialized (baseUrl={})", properties.resolvedBaseUrl());
    }

    public List<EcosDataRow> fetchStatistic(String statCode, String itemCode1,
                                             String cycle, String startDate,
                                             String endDate, int maxRows) {

        String uri = String.format("/%s/json/kr/1/%d/%s/%s/%s/%s/%s",
                properties.key(), maxRows, statCode, cycle, startDate, endDate, itemCode1);

        try {
            String body = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(2, java.time.Duration.ofSeconds(1))
                            .maxBackoff(java.time.Duration.ofSeconds(5)))
                    .block(java.time.Duration.ofSeconds(30));

            return parseResponse(body, statCode);
        } catch (Exception e) {
            log.error("ECOS API call failed for statCode={}, itemCode1={}: {}",
                    statCode, itemCode1, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<EcosDataRow> parseResponse(String body, String statCode) {
        try {
            JsonNode root = objectMapper.readTree(body);

            JsonNode result = root.path("RESULT");
            if (!result.isMissingNode()) {
                String code = result.path("CODE").asText();
                String message = result.path("MESSAGE").asText();
                log.warn("ECOS API returned error for statCode={}: {} - {}", statCode, code, message);
                return Collections.emptyList();
            }

            JsonNode rows = root.path("StatisticSearch").path("row");
            if (rows.isMissingNode() || !rows.isArray()) {
                log.debug("No rows in ECOS response for statCode={}", statCode);
                return Collections.emptyList();
            }

            List<EcosDataRow> dataRows = new ArrayList<>();
            for (JsonNode row : rows) {
                String dataValue = row.path("DATA_VALUE").asText();

                if (dataValue.isBlank() || "-".equals(dataValue)) {
                    continue;
                }

                dataRows.add(new EcosDataRow(
                        row.path("TIME").asText(),
                        dataValue,
                        row.path("UNIT_NAME").asText(),
                        row.path("ITEM_NAME1").asText(),
                        row.path("STAT_NAME").asText()
                ));
            }
            return dataRows;
        } catch (Exception e) {
            log.error("Failed to parse ECOS API response for statCode={}: {}", statCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    public record EcosDataRow(String time, String dataValue, String unitName,
                               String itemName, String statName) {

        public Double valueAsDouble() {
            try {
                return Double.parseDouble(dataValue.replace(",", ""));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
