package com.fineasy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dart.api")
public record DartApiProperties(
        String key,
        String baseUrl
) {
    private static final String DEFAULT_BASE_URL = "https://opendart.fss.or.kr/api";

    public String resolvedBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }
        return DEFAULT_BASE_URL;
    }
}
