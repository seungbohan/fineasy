package com.fineasy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sec.edgar")
public record SecEdgarApiProperties(
        String userAgent,
        String baseUrl
) {
    private static final String DEFAULT_BASE_URL = "https://data.sec.gov";
    private static final String DEFAULT_USER_AGENT = "FinEasy/1.0 contact@fineasy.co.kr";

    public String resolvedBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }
        return DEFAULT_BASE_URL;
    }

    public String resolvedUserAgent() {
        if (userAgent != null && !userAgent.isBlank()) {
            return userAgent;
        }
        return DEFAULT_USER_AGENT;
    }
}
