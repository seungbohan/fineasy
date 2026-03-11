package com.fineasy.external.kis;

import com.fineasy.config.KisApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "data-provider.type", havingValue = "kis")
public class KisApiClient {

    private static final Logger log = LoggerFactory.getLogger(KisApiClient.class);

    private final WebClient webClient;

    private final KisApiProperties properties;

    private final KisTokenManager tokenManager;

    public KisApiClient(@Qualifier("kisWebClient") WebClient webClient,
                        KisApiProperties properties,
                        KisTokenManager tokenManager) {
        this.webClient = webClient;
        this.properties = properties;
        this.tokenManager = tokenManager;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String path, String trId, Map<String, String> queryParams) {
        String resolvedTrId = properties.resolveTrId(trId);

        try {
            Map<String, Object> response = executeGet(path, resolvedTrId, queryParams);
            validateResponse(response);
            return response;

        } catch (KisApiException e) {
            if (isAuthError(e)) {
                log.warn("KIS API authentication error, retrying with refreshed token");
                tokenManager.refreshToken();
                Map<String, Object> retryResponse = executeGet(path, resolvedTrId, queryParams);
                validateResponse(retryResponse);
                return retryResponse;
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeGet(String path, String trId, Map<String, String> queryParams) {
        String token = tokenManager.getAccessToken();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        queryParams.forEach(params::add);

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParams(params)
                            .build())
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.appkey())
                    .header("appsecret", properties.appsecret())
                    .header("tr_id", trId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("KIS API 4xx error [{}]: {}", clientResponse.statusCode(), body);
                                        return Mono.error(new KisApiException(
                                                "KIS_CLIENT_ERROR",
                                                "KIS API client error: " + body,
                                                clientResponse.statusCode().value()));
                                    }))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("KIS API 5xx error [{}]: {}", clientResponse.statusCode(), body);
                                        return Mono.error(new KisApiException(
                                                "KIS_SERVER_ERROR",
                                                "KIS API server error: " + body,
                                                clientResponse.statusCode().value()));
                                    }))
                    .bodyToMono(Map.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(1, java.time.Duration.ofSeconds(1))
                            .filter(ex -> ex instanceof KisApiException kae
                                    && "KIS_SERVER_ERROR".equals(kae.getErrorCode())))
                    .block(java.time.Duration.ofSeconds(30));

        } catch (KisApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("KIS API request failed: {} {}", path, e.getMessage());
            throw new KisApiException("KIS_REQUEST_ERROR", "KIS API request failed: " + e.getMessage(), e);
        }
    }

    private void validateResponse(Map<String, Object> response) {
        if (response == null) {
            throw new KisApiException("KIS_EMPTY_RESPONSE", "KIS API returned an empty response");
        }

        Object rtCd = response.get("rt_cd");
        if (rtCd != null && !"0".equals(rtCd.toString())) {
            String msg1 = response.getOrDefault("msg1", "Unknown error").toString();
            String msgCd = response.getOrDefault("msg_cd", "UNKNOWN").toString();
            log.error("KIS API business error: rt_cd={}, msg_cd={}, msg1={}", rtCd, msgCd, msg1);
            throw new KisApiException(msgCd, "KIS API error: " + msg1);
        }
    }

    private boolean isAuthError(KisApiException e) {
        return e.getHttpStatus() == 401
                || "EGW00123".equals(e.getErrorCode())
                || "EGW00121".equals(e.getErrorCode());
    }
}
