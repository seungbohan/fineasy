package com.fineasy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fred.api")
public record FredApiProperties(
        String key,
        String baseUrl
) {
    private static final String DEFAULT_BASE_URL = "https://api.stlouisfed.org/fred";

    public String resolvedBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }
        return DEFAULT_BASE_URL;
    }
}
