package com.fineasy.external.dart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.config.DartApiProperties;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Component
@ConditionalOnExpression("!'${dart.api.key:}'.isEmpty()")
public class DartApiClient {

    private static final Logger log = LoggerFactory.getLogger(DartApiClient.class);

    private final DartApiProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public DartApiClient(DartApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.resolvedBaseUrl())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024));

        try {
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .protocols("TLSv1.2", "TLSv1.3")
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(spec -> spec.sslContext(sslContext));

            builder.clientConnector(new ReactorClientHttpConnector(httpClient));
        } catch (Exception e) {
            log.error("Failed to configure SSL for DART WebClient: {}", e.getMessage());
        }

        this.webClient = builder.build();

        log.info("DART API client initialized with base URL: {}", properties.resolvedBaseUrl());
    }

    public byte[] downloadCorpCodeZip() {
        try {
            byte[] zipBytes = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/corpCode.xml")
                            .queryParam("crtfc_key", properties.key())
                            .build())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .retryWhen(reactor.util.retry.Retry.backoff(2, java.time.Duration.ofSeconds(1))
                            .maxBackoff(java.time.Duration.ofSeconds(5)))
                    .block(java.time.Duration.ofSeconds(30));

            if (zipBytes == null || zipBytes.length == 0) {
                log.warn("DART corpCode.xml download returned empty response");
                return null;
            }

            log.info("DART corpCode.xml ZIP downloaded: {} bytes", zipBytes.length);
            return zipBytes;
        } catch (Exception e) {
            log.error("Failed to download DART corpCode.xml: {}", e.getMessage());
            return null;
        }
    }

    public JsonNode fetchSingleCompanyAccount(String corpCode, String bsnsYear,
                                               String reprtCode, String fsDiv) {
        try {
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/fnlttSinglAcnt.json")
                            .queryParam("crtfc_key", properties.key())
                            .queryParam("corp_code", corpCode)
                            .queryParam("bsns_year", bsnsYear)
                            .queryParam("reprt_code", reprtCode)
                            .queryParam("fs_div", fsDiv)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(2, java.time.Duration.ofSeconds(1))
                            .maxBackoff(java.time.Duration.ofSeconds(5)))
                    .block(java.time.Duration.ofSeconds(30));

            if (body == null || body.isBlank()) {
                log.warn("DART fnlttSinglAcnt returned empty response for corpCode={}, year={}",
                        corpCode, bsnsYear);
                return null;
            }

            JsonNode root = objectMapper.readTree(body);

            String status = root.path("status").asText("");
            if (!"000".equals(status)) {
                String message = root.path("message").asText("Unknown error");
                log.warn("DART API error for corpCode={}, year={}, fsDiv={}: [{}] {}",
                        corpCode, bsnsYear, fsDiv, status, message);
                return null;
            }

            return root;
        } catch (Exception e) {
            log.error("DART fnlttSinglAcnt API call failed for corpCode={}, year={}: {}",
                    corpCode, bsnsYear, e.getMessage());
            return null;
        }
    }

    public JsonNode fetchSingleCompanyAccountAll(String corpCode, String bsnsYear,
                                                  String reprtCode, String fsDiv) {
        try {
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/fnlttSinglAcntAll.json")
                            .queryParam("crtfc_key", properties.key())
                            .queryParam("corp_code", corpCode)
                            .queryParam("bsns_year", bsnsYear)
                            .queryParam("reprt_code", reprtCode)
                            .queryParam("fs_div", fsDiv)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(2, java.time.Duration.ofSeconds(1))
                            .maxBackoff(java.time.Duration.ofSeconds(5)))
                    .block(java.time.Duration.ofSeconds(30));

            if (body == null || body.isBlank()) {
                log.warn("DART fnlttSinglAcntAll returned empty response for corpCode={}, year={}",
                        corpCode, bsnsYear);
                return null;
            }

            JsonNode root = objectMapper.readTree(body);

            String status = root.path("status").asText("");
            if (!"000".equals(status)) {
                String message = root.path("message").asText("Unknown error");
                log.warn("DART API error (All) for corpCode={}, year={}, fsDiv={}: [{}] {}",
                        corpCode, bsnsYear, fsDiv, status, message);
                return null;
            }

            return root;
        } catch (Exception e) {
            log.error("DART fnlttSinglAcntAll API call failed for corpCode={}, year={}: {}",
                    corpCode, bsnsYear, e.getMessage());
            return null;
        }
    }
}
