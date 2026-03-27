package com.fineasy.service;

import com.fineasy.dto.response.*;
import com.fineasy.entity.Market;
import com.fineasy.entity.StockEntity;
import com.fineasy.entity.StockPriceEntity;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.external.dart.DartFinancialService;
import com.fineasy.external.sec.SecEdgarFundamentalsService;
import com.fineasy.repository.StockPriceRepository;
import com.fineasy.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Transactional(readOnly = true)
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final StockRepository stockRepository;

    private final StockPriceRepository stockPriceRepository;

    private static final int MAX_PEER_STOCKS = 10;

    private static final long PEER_API_TIMEOUT_SECONDS = 10;

    private static final double VALUATION_THRESHOLD = 0.20;

    private final StockDataProvider stockDataProvider;

    @Autowired(required = false)
    private DartFinancialService dartFinancialService;

    @Autowired(required = false)
    private SecEdgarFundamentalsService secEdgarFundamentalsService;

    public StockService(StockRepository stockRepository,
                        StockPriceRepository stockPriceRepository,
                        StockDataProvider stockDataProvider) {
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.stockDataProvider = stockDataProvider;
    }

    public List<StockResponse> searchStocks(String query) {
        return stockRepository.searchByNameOrCode(query).stream()
                .map(this::toStockResponse)
                .toList();
    }

    @Cacheable(value = "popular-stocks", unless = "#result == null || #result.isEmpty()")
    public List<StockResponse> getPopularStocks() {
        return stockRepository.findPopularStocks(PageRequest.of(0, 20)).stream()
                .map(this::toStockResponse)
                .toList();
    }

    public List<StockResponse> getStocksByRegion(String region, int page, int size) {
        List<com.fineasy.entity.Market> markets;
        if ("overseas".equals(region)) {
            markets = List.of(com.fineasy.entity.Market.NASDAQ, com.fineasy.entity.Market.NYSE);
        } else {
            markets = List.of(com.fineasy.entity.Market.KRX, com.fineasy.entity.Market.KOSDAQ);
        }
        return stockRepository.findByMarkets(markets, PageRequest.of(page, size)).stream()
                .map(this::toStockResponse)
                .toList();
    }

    @Cacheable(value = "stock-info", key = "#stockCode", unless = "#result == null")
    public StockResponse getStockInfo(String stockCode) {
        StockEntity stock = getStockEntityByCode(stockCode);
        return toStockResponse(stock);
    }

    @Cacheable(value = "stock-price", key = "#stockCode", unless = "#result == null")
    public StockPriceResponse getStockPrice(String stockCode) {
        StockEntity stock = getStockEntityByCode(stockCode);

        try {
            return stockDataProvider.getStockPriceDetail(stockCode, stock.getStockName());
        } catch (Exception e) {
            log.warn("[StockService] KIS API failed for {}, falling back to DB: {}", stockCode, e.getMessage());
        }

        List<StockPriceEntity> prices = stockPriceRepository.findLatestByStockId(
                stock.getId(), PageRequest.of(0, 2));

        if (!prices.isEmpty()) {
            StockPriceEntity latest = prices.get(0);
            BigDecimal currentPrice = latest.getClosePrice();
            BigDecimal changeAmount;
            double changeRate;

            if (prices.size() >= 2) {

                BigDecimal prevClose = prices.get(1).getClosePrice();
                changeAmount = currentPrice.subtract(prevClose);
                changeRate = prevClose.compareTo(BigDecimal.ZERO) > 0
                        ? changeAmount.doubleValue() / prevClose.doubleValue() * 100
                        : 0.0;
            } else {
                changeAmount = BigDecimal.ZERO;
                changeRate = 0.0;
            }

            return new StockPriceResponse(
                    stock.getStockCode(),
                    stock.getStockName(),
                    currentPrice,
                    changeAmount,
                    changeRate,
                    latest.getVolume() != null ? latest.getVolume() : 0L,
                    latest.getTradeDate()
            );
        }

        return new StockPriceResponse(
                stock.getStockCode(),
                stock.getStockName(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0.0,
                0L,
                LocalDate.now()
        );
    }

    @Cacheable(value = "stock-chart", key = "#stockCode + ':' + #period + ':' + #type", unless = "#result == null")
    public StockChartResponse getChartData(String stockCode, String period, String type) {
        StockEntity stock = getStockEntityByCode(stockCode);

        List<StockChartResponse.CandleData> candles = null;

        if ("1D".equals(period)) {
            candles = fetchMinuteCandles(stockCode);
        }

        if (candles == null || candles.isEmpty()) {
            candles = fetchDailyCandles(stock, period);
        }

        Map<String, List<Double>> indicators = calculateIndicators(candles);

        return new StockChartResponse(
                stock.getStockCode(),
                stock.getStockName(),
                period,
                candles,
                indicators
        );
    }

    private List<StockChartResponse.CandleData> fetchMinuteCandles(String stockCode) {
        try {
            List<StockChartResponse.CandleData> candles = stockDataProvider.getMinutePrices(stockCode);
            if (candles != null && !candles.isEmpty()) {
                return candles;
            }
        } catch (Exception e) {

        }
        return null;
    }

    private List<StockChartResponse.CandleData> fetchDailyCandles(StockEntity stock, String period) {
        LocalDate now = LocalDate.now();
        LocalDate from = calculateFromDate(now, period);

        List<StockChartResponse.CandleData> candles = null;
        try {
            candles = stockDataProvider.getDailyPrices(stock.getStockCode(), from, now);
        } catch (Exception e) {

        }

        if (candles == null || candles.isEmpty()) {
            List<StockPriceEntity> prices = stockPriceRepository
                    .findByStockIdAndDateRange(stock.getId(), from, now);

            candles = prices.stream()
                    .map(p -> new StockChartResponse.CandleData(
                            p.getTradeDate().toString(), p.getOpenPrice(), p.getHighPrice(),
                            p.getLowPrice(), p.getClosePrice(),
                            p.getVolume() != null ? p.getVolume() : 0L
                    ))
                    .toList();
        }

        return candles;
    }

    @Cacheable(value = "stock-financials", key = "#stockCode", unless = "#result == null")
    public StockFinancialsResponse getFinancials(String stockCode) {
        StockEntity stock = getStockEntityByCode(stockCode);
        StockFinancialsResponse response = stockDataProvider.getFinancials(stockCode);

        return new StockFinancialsResponse(
                response.stockCode(),
                stock.getStockName(),
                response.marketCap(),
                response.sharesOutstanding(),
                response.per(),
                response.pbr(),
                response.eps(),
                response.dividendYield(),
                response.high52Week(),
                response.low52Week()
        );
    }

    @Cacheable(value = "stock-fundamentals", key = "#stockCode + ':dart'", unless = "#result == null")
    @Transactional
    public DartFundamentalsResponse getDartFundamentals(String stockCode) {
        return callFinancialService(stockCode, "fundamentals",
                (code, name) -> dartFinancialService.getFundamentals(code, name),
                (code, name) -> secEdgarFundamentalsService.getFundamentals(code, name));
    }

    @Cacheable(value = "stock-fundamentals", key = "#stockCode + ':multi'", unless = "#result == null")
    @Transactional
    public MultiYearFundamentalsResponse getMultiYearFundamentals(String stockCode) {
        return callFinancialService(stockCode, "multi-year fundamentals",
                (code, name) -> dartFinancialService.getMultiYearFundamentals(code, name),
                (code, name) -> secEdgarFundamentalsService.getMultiYearFundamentals(code, name));
    }

    private <T> T callFinancialService(String stockCode, String label,
                                        java.util.function.BiFunction<String, String, T> dartCall,
                                        java.util.function.BiFunction<String, String, T> secEdgarCall) {
        StockEntity stock = getStockEntityByCode(stockCode);

        // Overseas stocks -> SEC EDGAR
        if (stock.getMarket() == Market.NASDAQ || stock.getMarket() == Market.NYSE
                || stock.getMarket() == Market.AMEX) {
            if (secEdgarFundamentalsService == null) {
                log.debug("SEC EDGAR fundamentals service not available for overseas stock: {}", stockCode);
                return null;
            }
            try {
                return secEdgarCall.apply(stockCode, stock.getStockName());
            } catch (Exception e) {
                log.error("Failed to fetch {} from SEC EDGAR for stockCode={}: {}", label, stockCode, e.getMessage());
                return null;
            }
        }

        // Domestic stocks -> DART
        if (dartFinancialService == null) {
            log.debug("DART financial service is not available (API key not configured)");
            return null;
        }

        try {
            return dartCall.apply(stockCode, stock.getStockName());
        } catch (Exception e) {
            log.error("Failed to fetch {} for stockCode={}: {}", label, stockCode, e.getMessage());
            return null;
        }
    }

    @Cacheable(value = "sector-comparison", key = "#stockCode", unless = "#result == null")
    public SectorComparisonResponse getSectorComparison(String stockCode) {
        StockEntity stock = getStockEntityByCode(stockCode);

        String sector = stock.getSector();
        if (sector == null || sector.isBlank()) {
            log.debug("No sector assigned for stockCode={}", stockCode);
            return null;
        }

        List<StockEntity> sectorStocks = findPeerStocks(stockCode, sector);
        if (sectorStocks.isEmpty()) {
            return null;
        }

        StockFinancialsResponse targetFinancials = fetchFinancialsSafely(stockCode);
        Double currentPer = targetFinancials != null ? targetFinancials.per() : null;
        Double currentPbr = targetFinancials != null ? targetFinancials.pbr() : null;

        List<StockFinancialsResponse> peerFinancials = fetchPeerFinancials(sectorStocks);
        if (peerFinancials.isEmpty()) {
            log.debug("No peer financial data collected for sector={}", sector);
            return null;
        }

        Double sectorAvgPer = calculateSectorAverage(currentPer, peerFinancials, StockFinancialsResponse::per);
        Double sectorAvgPbr = calculateSectorAverage(currentPbr, peerFinancials, StockFinancialsResponse::pbr);

        return new SectorComparisonResponse(
                stockCode, stock.getStockName(), sector,
                currentPer, currentPbr, sectorAvgPer, sectorAvgPbr,
                evaluateRelativePosition(currentPer, sectorAvgPer),
                evaluateRelativePosition(currentPbr, sectorAvgPbr),
                peerFinancials.size()
        );
    }

    private List<StockEntity> findPeerStocks(String stockCode, String sector) {
        List<StockEntity> peers = stockRepository.findBySector(sector).stream()
                .filter(s -> !s.getStockCode().equals(stockCode))
                .toList();
        if (peers.isEmpty()) {
            log.debug("No peer stocks found for sector={}", sector);
            return List.of();
        }
        int maxPeers = Math.min(peers.size(), MAX_PEER_STOCKS);
        return peers.subList(0, maxPeers);
    }

    private StockFinancialsResponse fetchFinancialsSafely(String stockCode) {
        try {
            return stockDataProvider.getFinancials(stockCode);
        } catch (Exception e) {
            log.warn("Failed to fetch financials for stockCode={}: {}", stockCode, e.getMessage());
            return null;
        }
    }

    private List<StockFinancialsResponse> fetchPeerFinancials(List<StockEntity> peers) {
        List<CompletableFuture<StockFinancialsResponse>> futures = peers.stream()
                .map(peer -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return stockDataProvider.getFinancials(peer.getStockCode());
                    } catch (Exception e) {
                        log.debug("Failed to fetch financials for peer stockCode={}: {}",
                                peer.getStockCode(), e.getMessage());
                        return null;
                    }
                }))
                .toList();

        List<StockFinancialsResponse> results = new ArrayList<>();
        for (CompletableFuture<StockFinancialsResponse> future : futures) {
            try {
                StockFinancialsResponse result = future.get(PEER_API_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                log.debug("Peer financial fetch timed out or failed: {}", e.getMessage());
            }
        }
        return results;
    }

    private Double calculateSectorAverage(Double targetValue,
                                          List<StockFinancialsResponse> peers,
                                          java.util.function.Function<StockFinancialsResponse, Double> extractor) {
        double sum = 0;
        int count = 0;
        if (targetValue != null && targetValue > 0) {
            sum += targetValue;
            count++;
        }
        for (StockFinancialsResponse pf : peers) {
            Double val = extractor.apply(pf);
            if (val != null && val > 0) {
                sum += val;
                count++;
            }
        }
        return count > 0 ? Math.round(sum / count * 100.0) / 100.0 : null;
    }

    private String evaluateRelativePosition(Double current, Double sectorAvg) {
        if (current == null || sectorAvg == null || sectorAvg == 0) {
            return "판단 불가";
        }
        double ratio = current / sectorAvg;
        if (ratio < (1 - VALUATION_THRESHOLD)) {
            return "저평가";
        } else if (ratio > (1 + VALUATION_THRESHOLD)) {
            return "고평가";
        } else {
            return "적정";
        }
    }

    public StockEntity getStockEntityByCode(String stockCode) {
        return stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new EntityNotFoundException("Stock", stockCode));
    }

    public StockEntity getStockEntityById(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stock", String.valueOf(id)));
    }

    public PriceSummaryResponse getRecentPriceSummary(Long stockId, int days) {
        List<StockPriceEntity> prices = stockPriceRepository
                .findLatestByStockId(stockId, PageRequest.of(0, days));

        if (prices.isEmpty()) {
            return PriceSummaryResponse.EMPTY;
        }

        BigDecimal highest = prices.stream()
                .map(StockPriceEntity::getHighPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::max)
                .orElse(BigDecimal.ZERO);

        BigDecimal lowest = prices.stream()
                .map(StockPriceEntity::getLowPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::min)
                .orElse(BigDecimal.ZERO);

        return new PriceSummaryResponse(
                prices.get(0).getClosePrice(),
                prices.get(prices.size() - 1).getClosePrice(),
                highest,
                lowest
        );
    }

    private StockResponse toStockResponse(StockEntity stock) {
        return new StockResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getMarket(),
                stock.getSector()
        );
    }

    private LocalDate calculateFromDate(LocalDate now, String period) {
        return switch (period) {
            case "1D" -> now.minusDays(1);
            case "1W" -> now.minusWeeks(1);
            case "1M" -> now.minusMonths(1);
            case "3M" -> now.minusMonths(3);
            case "1Y" -> now.minusYears(1);
            case "ALL" -> now.minusYears(10);
            default -> now.minusMonths(1);
        };
    }

    private Map<String, List<Double>> calculateIndicators(List<StockChartResponse.CandleData> candles) {
        Map<String, List<Double>> indicators = new LinkedHashMap<>();
        List<Double> closePrices = candles.stream()
                .map(c -> c.close().doubleValue())
                .toList();

        indicators.put("ma5", calculateMA(closePrices, 5));
        indicators.put("ma20", calculateMA(closePrices, 20));
        indicators.put("ma60", calculateMA(closePrices, 60));

        return indicators;
    }

    private List<Double> calculateMA(List<Double> prices, int window) {
        List<Double> result = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            if (i < window - 1) {
                result.add(null);
            } else {
                double sum = 0;
                for (int j = i - window + 1; j <= i; j++) {
                    sum += prices.get(j);
                }
                result.add(Math.round(sum / window * 100.0) / 100.0);
            }
        }
        return result;
    }
}
