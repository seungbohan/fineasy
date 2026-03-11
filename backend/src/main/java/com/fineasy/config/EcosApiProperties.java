package com.fineasy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecos.api")
public record EcosApiProperties(
        String key,
        String baseUrl
) {
    private static final String DEFAULT_BASE_URL = "https://ecos.bok.or.kr/api/StatisticSearch";

    public String resolvedBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }
        return DEFAULT_BASE_URL;
    }
}
