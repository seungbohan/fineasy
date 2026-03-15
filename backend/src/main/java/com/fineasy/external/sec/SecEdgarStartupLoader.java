package com.fineasy.external.sec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SecEdgarStartupLoader {

    private static final Logger log = LoggerFactory.getLogger(SecEdgarStartupLoader.class);

    private final SecEdgarApiClient secEdgarApiClient;

    public SecEdgarStartupLoader(SecEdgarApiClient secEdgarApiClient) {
        this.secEdgarApiClient = secEdgarApiClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Loading SEC EDGAR company tickers on startup...");
        try {
            secEdgarApiClient.loadCompanyTickers();
        } catch (Exception e) {
            log.error("Failed to load SEC company tickers on startup: {}", e.getMessage());
        }
    }
}
