package com.fineasy.external.kis;

import com.fineasy.config.KisApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@ConditionalOnProperty(name = "data-provider.type", havingValue = "kis")
public class KisTokenManager {

    private static final Logger log = LoggerFactory.getLogger(KisTokenManager.class);

    private static final String TOKEN_PATH = "/oauth2/tokenP";
    private static final String GRANT_TYPE = "client_credentials";

    private static final long REFRESH_MARGIN_SECONDS = 3600;

    private final WebClient webClient;
    private final KisApiProperties properties;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile String accessToken;
    private volatile Instant tokenExpiresAt;

    public KisTokenManager(@Qualifier("kisWebClient") WebClient webClient,
                           KisApiProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public String getAccessToken() {
        lock.readLock().lock();
        try {
            if (isTokenValid()) {
                return accessToken;
            }
        } finally {
            lock.readLock().unlock();
        }

        return refreshToken();
    }

    public String refreshToken() {
        lock.writeLock().lock();
        try {

            if (isTokenValid()) {
                return accessToken;
            }

            log.info("Acquiring new KIS API access token");
            return acquireToken();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Scheduled(fixedDelay = 1800000)
    public void scheduledTokenRefresh() {
        lock.readLock().lock();
        try {
            if (accessToken == null) {
                return;
            }
            if (isTokenExpiringSoon()) {
                log.info("Token expiring soon, proactively refreshing");
            } else {
                return;
            }
        } finally {
            lock.readLock().unlock();
        }

        refreshToken();
    }

    private boolean isTokenValid() {
        return accessToken != null
                && tokenExpiresAt != null
                && Instant.now().isBefore(tokenExpiresAt.minusSeconds(REFRESH_MARGIN_SECONDS));
    }

    private boolean isTokenExpiringSoon() {
        if (tokenExpiresAt == null) {
            return true;
        }

        return Instant.now().isAfter(tokenExpiresAt.minusSeconds(7200));
    }

    @SuppressWarnings("unchecked")
    private String acquireToken() {
        try {
            Map<String, Object> requestBody = Map.of(
                    "grant_type", GRANT_TYPE,
                    "appkey", properties.appkey(),
                    "appsecret", properties.appsecret()
            );

            Map<String, Object> response = webClient.post()
                    .uri(TOKEN_PATH)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(java.time.Duration.ofSeconds(30));

            if (response == null || !response.containsKey("access_token")) {
                throw new KisApiException("TOKEN_ERROR", "Failed to acquire KIS access token: empty response");
            }

            String newToken = (String) response.get("access_token");
            int expiresIn = response.get("expires_in") instanceof Number number
                    ? number.intValue()
                    : 86400;

            this.tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
            this.accessToken = newToken;

            log.info("KIS access token acquired successfully, expires at: {}", tokenExpiresAt);
            return this.accessToken;

        } catch (KisApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to acquire KIS access token", e);
            throw new KisApiException("TOKEN_ERROR", "Failed to acquire KIS access token: " + e.getMessage(), e);
        }
    }
}
