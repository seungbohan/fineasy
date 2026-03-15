package com.fineasy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finnhub.api")
public record FinnhubApiProperties(
        String key,
        String baseUrl
) {
    private static final String DEFAULT_BASE_URL = "https://finnhub.io/api/v1";

    public String resolvedBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }
        return DEFAULT_BASE_URL;
    }
}
