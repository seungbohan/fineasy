package com.fineasy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kis.api")
public record KisApiProperties(
        String baseUrl,
        String appkey,
        String appsecret,
        String accountNumber,
        String environment
) {
    private static final String PROD_BASE_URL = "https://openapi.koreainvestment.com:9443";
    private static final String VTS_BASE_URL = "https://openapivts.koreainvestment.com:29443";

    public String prodBaseUrl() {
        return PROD_BASE_URL;
    }

    public String vtsBaseUrl() {
        return VTS_BASE_URL;
    }

    public String resolvedBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }
        return isVirtualTrading() ? VTS_BASE_URL : PROD_BASE_URL;
    }

    public boolean isVirtualTrading() {
        return "vts".equalsIgnoreCase(environment);
    }

    public String resolveTrId(String prodTrId, boolean isOrderApi) {
        if (isVirtualTrading() && isOrderApi) {
            return "V" + prodTrId;
        }
        return prodTrId;
    }

    @Deprecated
    public String resolveTrId(String prodTrId) {
        return resolveTrId(prodTrId, false);
    }
}
