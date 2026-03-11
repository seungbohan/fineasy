package com.fineasy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.dto.response.AnalysisReportResponse;
import com.fineasy.dto.response.PredictionResponse;
import com.fineasy.entity.*;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.repository.StockAnalysisReportRepository;
import com.fineasy.repository.StockPredictionRepository;
import com.fineasy.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private static final Duration AI_CACHE_TTL = Duration.ofHours(6);

    private static final String REPORT_CACHE_PREFIX = "analysis:report:";
    private static final String PREDICTION_CACHE_PREFIX = "analysis:prediction:";

    private final StockRepository stockRepository;
    private final StockAnalysisReportRepository reportRepository;
    private final StockPredictionRepository predictionRepository;
    private final AiAnalysisProvider aiAnalysisProvider;
    private final ObjectMapper objectMapper;
    private final RedisCacheHelper redisCacheHelper;

    public AnalysisService(StockRepository stockRepository,
                           StockAnalysisReportRepository reportRepository,
                           StockPredictionRepository predictionRepository,
                           AiAnalysisProvider aiAnalysisProvider,
                           ObjectMapper objectMapper,
                           RedisCacheHelper redisCacheHelper) {
        this.stockRepository = stockRepository;
        this.reportRepository = reportRepository;
        this.predictionRepository = predictionRepository;
        this.aiAnalysisProvider = aiAnalysisProvider;
        this.objectMapper = objectMapper;
        this.redisCacheHelper = redisCacheHelper;
    }

    @Transactional
    public AnalysisReportResponse getReport(String stockCode) {
        StockEntity stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new EntityNotFoundException("Stock", stockCode));

        LocalDate today = LocalDate.now();
        String cacheKey = REPORT_CACHE_PREFIX + stockCode + ":" + today;

        AnalysisReportResponse cached = redisCacheHelper.getFromCache(cacheKey, AnalysisReportResponse.class);
        if (cached != null) {
            log.debug("Report cache hit (Redis) for stock: {}", stockCode);
            return cached;
        }

        Optional<StockAnalysisReportEntity> dbReport =
                reportRepository.findByStockIdAndReportDate(stock.getId(), today);
        if (dbReport.isPresent()) {
            log.debug("Report cache hit (DB) for stock: {}", stockCode);
            AnalysisReportResponse fromDb = mapReportEntityToResponse(stockCode, dbReport.get());
            redisCacheHelper.putToCache(cacheKey, fromDb, AI_CACHE_TTL);
            return fromDb;
        }

        log.info("Report cache miss - generating via AI for stock: {}", stockCode);
        AnalysisReportResponse generated = aiAnalysisProvider.generateReport(stockCode);

        saveReportToDb(stock, today, generated);

        redisCacheHelper.putToCache(cacheKey, generated, AI_CACHE_TTL);

        return generated;
    }

    @Transactional
    public PredictionResponse getPrediction(String stockCode, String period) {
        StockEntity stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new EntityNotFoundException("Stock", stockCode));

        LocalDate today = LocalDate.now();
        String cacheKey = PREDICTION_CACHE_PREFIX + stockCode + ":" + period + ":" + today;

        PredictionResponse cached = redisCacheHelper.getFromCache(cacheKey, PredictionResponse.class);
        if (cached != null) {
            log.debug("Prediction cache hit (Redis) for stock: {}, period: {}", stockCode, period);
            return cached;
        }

        TargetPeriod targetPeriod = "1W".equals(period) ? TargetPeriod.ONE_WEEK : TargetPeriod.ONE_DAY;
        Optional<StockPredictionEntity> dbPrediction =
                predictionRepository.findByStockIdAndPredictionDateAndTargetPeriod(
                        stock.getId(), today, targetPeriod);

        if (dbPrediction.isPresent()) {
            log.debug("Prediction cache hit (DB) for stock: {}, period: {}", stockCode, period);
            PredictionResponse fromDb = mapPredictionEntityToResponse(stockCode, period, dbPrediction.get());
            redisCacheHelper.putToCache(cacheKey, fromDb, AI_CACHE_TTL);
            return fromDb;
        }

        log.info("Prediction cache miss - generating via AI for stock: {}, period: {}", stockCode, period);
        PredictionResponse generated = aiAnalysisProvider.generatePrediction(stockCode, period);

        savePredictionToDb(stock, today, targetPeriod, generated);

        redisCacheHelper.putToCache(cacheKey, generated, AI_CACHE_TTL);

        return generated;
    }

    public List<AnalysisReportResponse> getHistory(String stockCode) {
        StockEntity stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new EntityNotFoundException("Stock", stockCode));

        List<StockAnalysisReportEntity> reports = reportRepository
                .findByStockIdOrderByDateDesc(stock.getId(), PageRequest.of(0, 30));

        return reports.stream()
                .map(r -> mapReportEntityToResponse(stockCode, r))
                .toList();
    }

    private AnalysisReportResponse mapReportEntityToResponse(String stockCode, StockAnalysisReportEntity entity) {
        return new AnalysisReportResponse(
                stockCode,
                entity.getGeneratedAt(),
                entity.getSummary(),
                entity.getDescription(),
                redisCacheHelper.parseJsonList(entity.getKeyPoints()),
                entity.getInvestmentOpinion() != null ? entity.getInvestmentOpinion() : "HOLD",
                entity.getDisclaimer(),
                redisCacheHelper.parseJsonMap(entity.getTechnicalSignals())
        );
    }

    private PredictionResponse mapPredictionEntityToResponse(String stockCode, String period,
                                                              StockPredictionEntity entity) {
        return new PredictionResponse(
                stockCode,
                period,
                entity.getDirection(),
                entity.getConfidence() != null ? entity.getConfidence() : 0,
                redisCacheHelper.parseJsonList(entity.getReasons()),
                entity.getDisclaimer(),
                entity.getGeneratedAt(),
                entity.getValuation()
        );
    }

    private void saveReportToDb(StockEntity stock, LocalDate today, AnalysisReportResponse response) {
        try {
            String keyPointsJson = objectMapper.writeValueAsString(response.keyPoints());
            String signalsJson = objectMapper.writeValueAsString(response.technicalSignals());

            StockAnalysisReportEntity entity = new StockAnalysisReportEntity(
                    null, stock, today,
                    response.summary(), response.description(),
                    keyPointsJson, signalsJson,
                    response.investmentOpinion(),
                    response.disclaimer(), LocalDateTime.now()
            );
            reportRepository.save(entity);
            log.debug("Saved analysis report to DB for stock: {}", stock.getStockCode());
        } catch (Exception e) {
            log.error("Failed to save report to DB for stock: {}", stock.getStockCode(), e);
        }
    }

    private void savePredictionToDb(StockEntity stock, LocalDate today,
                                     TargetPeriod targetPeriod, PredictionResponse response) {
        try {
            String reasonsJson = objectMapper.writeValueAsString(response.reasons());

            StockPredictionEntity entity = new StockPredictionEntity(
                    null, stock, today, targetPeriod,
                    response.direction(),
                    response.confidence(), reasonsJson,
                    response.valuation(),
                    response.disclaimer(), LocalDateTime.now()
            );
            predictionRepository.save(entity);
            log.debug("Saved prediction to DB for stock: {}, period: {}", stock.getStockCode(), targetPeriod);
        } catch (Exception e) {
            log.error("Failed to save prediction to DB for stock: {}", stock.getStockCode(), e);
        }
    }

}
