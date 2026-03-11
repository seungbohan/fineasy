package com.fineasy.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(KisApiProperties.class)
@ConditionalOnProperty(name = "data-provider.type", havingValue = "kis")
public class KisApiConfig {

    @Bean("kisWebClient")
    public WebClient kisWebClient(KisApiProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.prodBaseUrl())
                .defaultHeader("content-type", "application/json; charset=utf-8")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}
