package com.fineasy.service;

import com.fineasy.dto.response.CryptoMarketResponse;
import com.fineasy.entity.CryptoMarketDataEntity;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.external.coingecko.CoinDef;
import com.fineasy.repository.CryptoMarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CryptoMarketService {

    private static final Logger log = LoggerFactory.getLogger(CryptoMarketService.class);

    private final CryptoMarketDataRepository cryptoRepository;

    public CryptoMarketService(CryptoMarketDataRepository cryptoRepository) {
        this.cryptoRepository = cryptoRepository;
    }

    @Cacheable(value = "crypto-prices", unless = "#result == null")
    public CryptoMarketResponse getLatestPrices() {
        List<CryptoMarketDataEntity> latestList = cryptoRepository.findAllLatest();

        List<CryptoMarketResponse.CoinData> coins = latestList.stream()
                .map(this::toCoinData)
                .toList();

        Instant updatedAt = latestList.stream()
                .map(CryptoMarketDataEntity::getRecordedAt)
                .max(Instant::compareTo)
                .orElse(null);

        return new CryptoMarketResponse(coins, updatedAt);
    }

    public CryptoMarketResponse getCoinDetail(String symbol) {
        String upperSymbol = symbol.toUpperCase();

        CoinDef coinDef = CoinDef.findBySymbol(upperSymbol);
        if (coinDef == null) {
            throw new EntityNotFoundException("Crypto", upperSymbol);
        }

        CryptoMarketDataEntity entity = cryptoRepository.findLatestBySymbol(upperSymbol)
                .orElseThrow(() -> new EntityNotFoundException("CryptoMarketData", upperSymbol));

        return new CryptoMarketResponse(
                List.of(toCoinData(entity)),
                entity.getRecordedAt()
        );
    }

    public CryptoMarketResponse getCoinHistory(String symbol, int days) {
        String upperSymbol = symbol.toUpperCase();

        CoinDef coinDef = CoinDef.findBySymbol(upperSymbol);
        if (coinDef == null) {
            throw new EntityNotFoundException("Crypto", upperSymbol);
        }

        int maxRecords = days * 48;
        List<CryptoMarketDataEntity> history = cryptoRepository
                .findBySymbolOrderByRecordedAtDesc(upperSymbol, PageRequest.of(0, maxRecords));

        if (history.isEmpty()) {
            throw new EntityNotFoundException("CryptoMarketData history", upperSymbol);
        }

        List<CryptoMarketResponse.CoinData> coins = history.stream()
                .map(this::toCoinData)
                .toList();

        return new CryptoMarketResponse(coins, history.get(0).getRecordedAt());
    }

    private CryptoMarketResponse.CoinData toCoinData(CryptoMarketDataEntity entity) {
        return new CryptoMarketResponse.CoinData(
                entity.getSymbol(),
                entity.getName(),
                entity.getPriceUsd(),
                entity.getPriceKrw(),
                entity.getMarketCapUsd(),
                entity.getVolume24hUsd(),
                entity.getChange24h(),
                entity.getChange7d(),
                entity.getRecordedAt()
        );
    }
}
