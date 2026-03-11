package com.fineasy.external.coingecko;

import com.fineasy.entity.CryptoMarketDataEntity;
import com.fineasy.repository.CryptoMarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;

@Service
public class CoinGeckoCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(CoinGeckoCollectionScheduler.class);
    private static final String SOURCE = "coingecko";

    private final CoinGeckoClient coinGeckoClient;
    private final CryptoMarketDataRepository cryptoRepository;

    public CoinGeckoCollectionScheduler(CoinGeckoClient coinGeckoClient,
                                         CryptoMarketDataRepository cryptoRepository) {
        this.coinGeckoClient = coinGeckoClient;
        this.cryptoRepository = cryptoRepository;
    }

    @PostConstruct
    public void initialCollection() {
        log.info("Starting initial CoinGecko data collection...");
        collectCryptoPrices();
        log.info("Initial CoinGecko data collection completed.");
    }

    @Scheduled(fixedRate = 30 * 60 * 1000, initialDelay = 3 * 60 * 1000)
    @SchedulerLock(name = "coinGeckoCollection", lockAtLeastFor = "PT5M", lockAtMostFor = "PT25M")
    public void periodicCollection() {
        log.info("Starting periodic CoinGecko data collection...");
        collectCryptoPrices();
        log.info("Periodic CoinGecko data collection completed.");
    }

    @Transactional
    public void collectCryptoPrices() {
        List<CoinGeckoClient.CoinMarketData> prices = coinGeckoClient.fetchAllCoinPrices();

        if (prices.isEmpty()) {
            log.warn("No coin price data received from CoinGecko API");
            return;
        }

        Instant recordedAt = Instant.now();
        int saved = 0;

        for (CoinGeckoClient.CoinMarketData data : prices) {
            try {
                CryptoMarketDataEntity entity = new CryptoMarketDataEntity(
                        null,
                        data.symbol(),
                        data.name(),
                        data.priceUsd(),
                        data.priceKrw(),
                        data.marketCapUsd(),
                        data.volume24hUsd(),
                        data.change24h(),
                        null,
                        recordedAt,
                        SOURCE
                );
                cryptoRepository.save(entity);
                saved++;
            } catch (Exception e) {
                log.error("Failed to save crypto data for {}: {}", data.symbol(), e.getMessage());
            }
        }

        log.info("Collected and saved {} crypto market data points", saved);
    }
}
