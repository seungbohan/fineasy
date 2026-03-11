package com.fineasy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.dto.response.AnalysisReportResponse;
import com.fineasy.dto.response.PredictionResponse;
import com.fineasy.entity.*;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.repository.StockAnalysisReportRepository;
import com.fineasy.repository.StockPredictionRepository;
import com.fineasy.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockAnalysisReportRepository reportRepository;

    @Mock
    private StockPredictionRepository predictionRepository;

    @Mock
    private AiAnalysisProvider aiAnalysisProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AnalysisService analysisService;
    private ObjectMapper objectMapper;
    private StockEntity samsungStock;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        RedisCacheHelper redisCacheHelper = new RedisCacheHelper(objectMapper);
        redisCacheHelper.setRedisTemplate(redisTemplate);
        analysisService = new AnalysisService(
                stockRepository, reportRepository, predictionRepository,
                aiAnalysisProvider, objectMapper, redisCacheHelper
        );

        samsungStock = new StockEntity(1L, "005930", "삼성전자",
                Market.KRX, "반도체", true, LocalDateTime.now());
    }

    private AnalysisReportResponse sampleReport() {
        return new AnalysisReportResponse(
                "005930", LocalDateTime.now(),
                "삼성전자 단기 상승 신호",
                "현재 RSI가 45로 중립 구간에 있으며...",
                List.of("RSI 과매도 구간에서 회복 중", "20일 이동평균선 상향 돌파", "거래량 증가"),
                "HOLD",
                "이 분석은 투자 권유가 아닙니다.",
                Map.of("rsi", 45.2, "macd", "BULLISH")
        );
    }

    private PredictionResponse samplePrediction() {
        return new PredictionResponse(
                "005930", "1D", PredictionDirection.UP, 65,
                List.of("뉴스 감성 긍정", "펀더멘털 양호", "거래량 증가"),
                "이 예측은 투자 권유가 아닙니다.",
                LocalDateTime.now(),
                "FAIR"
        );
    }

    @Nested
    @DisplayName("getReport()")
    class ReportTests {

        @Test
        @DisplayName("Throws EntityNotFoundException for non-existent stock")
        void unknownStockThrows() {
            when(stockRepository.findByStockCode("UNKNOWN")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> analysisService.getReport("UNKNOWN"));
        }

        @Test
        @DisplayName("Returns cached report on Redis hit")
        void redisCacheHitSkipsDbAndAi() throws Exception {
            when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(samsungStock));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            AnalysisReportResponse cached = sampleReport();
            when(valueOperations.get(contains("report"))).thenReturn(
                    objectMapper.writeValueAsString(cached));

            AnalysisReportResponse result = analysisService.getReport("005930");

            assertEquals("005930", result.stockCode());
            verify(reportRepository, never()).findByStockIdAndReportDate(anyLong(), any());
            verify(aiAnalysisProvider, never()).generateReport(anyString());
        }

        @Test
        @DisplayName("Returns DB report on Redis miss, DB hit")
        void dbCacheHitSkipsAi() {
            when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(samsungStock));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            StockAnalysisReportEntity dbEntity = new StockAnalysisReportEntity(
                    1L, samsungStock, LocalDate.now(),
                    "요약", "설명",
                    "[\"포인트1\",\"포인트2\"]",
                    "{\"rsi\":45.0}",
                    "HOLD",
                    "면책조항", LocalDateTime.now()
            );
            when(reportRepository.findByStockIdAndReportDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.of(dbEntity));

            AnalysisReportResponse result = analysisService.getReport("005930");

            assertEquals("005930", result.stockCode());
            assertEquals("요약", result.summary());
            verify(aiAnalysisProvider, never()).generateReport(anyString());
            verify(valueOperations).set(contains("report"), anyString(), any());
        }

        @Test
        @DisplayName("Calls AI provider on both Redis and DB miss, saves to both")
        void fullCacheMissCallsAi() {
            when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(samsungStock));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(reportRepository.findByStockIdAndReportDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.empty());

            AnalysisReportResponse aiResponse = sampleReport();
            when(aiAnalysisProvider.generateReport("005930")).thenReturn(aiResponse);

            AnalysisReportResponse result = analysisService.getReport("005930");

            assertEquals(aiResponse.summary(), result.summary());
            verify(aiAnalysisProvider).generateReport("005930");
            verify(reportRepository).save(any(StockAnalysisReportEntity.class));
            verify(valueOperations).set(contains("report"), anyString(), any());
        }
    }

    @Nested
    @DisplayName("getPrediction()")
    class PredictionTests {

        @Test
        @DisplayName("Throws EntityNotFoundException for non-existent stock")
        void unknownStockThrows() {
            when(stockRepository.findByStockCode("UNKNOWN")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> analysisService.getPrediction("UNKNOWN", "1D"));
        }

        @Test
        @DisplayName("Returns cached prediction on Redis hit")
        void redisCacheHit() throws Exception {
            when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(samsungStock));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            PredictionResponse cached = samplePrediction();
            when(valueOperations.get(contains("prediction"))).thenReturn(
                    objectMapper.writeValueAsString(cached));

            PredictionResponse result = analysisService.getPrediction("005930", "1D");

            assertEquals("005930", result.stockCode());
            verify(predictionRepository, never())
                    .findByStockIdAndPredictionDateAndTargetPeriod(anyLong(), any(), any());
        }

        @Test
        @DisplayName("Uses ONE_WEEK target period for '1W' input")
        void oneWeekPeriodMapping() {
            when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(samsungStock));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            StockPredictionEntity dbEntity = new StockPredictionEntity(
                    1L, samsungStock, LocalDate.now(), TargetPeriod.ONE_WEEK,
                    PredictionDirection.DOWN, 40,
                    "[\"이유1\",\"이유2\"]", "FAIR",
                    "면책조항", LocalDateTime.now()
            );
            when(predictionRepository.findByStockIdAndPredictionDateAndTargetPeriod(
                    eq(1L), any(LocalDate.class), eq(TargetPeriod.ONE_WEEK)))
                    .thenReturn(Optional.of(dbEntity));

            PredictionResponse result = analysisService.getPrediction("005930", "1W");

            assertEquals("1W", result.period());
            assertEquals(PredictionDirection.DOWN, result.direction());
        }

        @Test
        @DisplayName("Calls AI provider on full cache miss")
        void fullCacheMissCallsAi() {
            when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(samsungStock));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(predictionRepository.findByStockIdAndPredictionDateAndTargetPeriod(
                    eq(1L), any(LocalDate.class), eq(TargetPeriod.ONE_DAY)))
                    .thenReturn(Optional.empty());

            PredictionResponse aiResponse = samplePrediction();
            when(aiAnalysisProvider.generatePrediction("005930", "1D")).thenReturn(aiResponse);

            PredictionResponse result = analysisService.getPrediction("005930", "1D");

            assertEquals(PredictionDirection.UP, result.direction());
            verify(aiAnalysisProvider).generatePrediction("005930", "1D");
            verify(predictionRepository).save(any(StockPredictionEntity.class));
        }
    }

    @Nested
    @DisplayName("Redis unavailable (graceful degradation)")
    class RedisUnavailableTests {

        @Test
        @DisplayName("Works without Redis when redisTemplate is null")
        void worksWithoutRedis() {
            RedisCacheHelper noRedisCacheHelper = new RedisCacheHelper(objectMapper);
            AnalysisService noRedisService = new AnalysisService(
                    stockRepository, reportRepository, predictionRepository,
                    aiAnalysisProvider, objectMapper, noRedisCacheHelper
            );

            when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(samsungStock));
            when(reportRepository.findByStockIdAndReportDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.empty());

            AnalysisReportResponse aiResponse = sampleReport();
            when(aiAnalysisProvider.generateReport("005930")).thenReturn(aiResponse);

            AnalysisReportResponse result = noRedisService.getReport("005930");

            assertEquals("005930", result.stockCode());
            verify(aiAnalysisProvider).generateReport("005930");
        }

        @Test
        @DisplayName("Gracefully handles Redis exception on read")
        void handlesRedisReadException() {
            when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(samsungStock));
            when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection refused"));
            when(reportRepository.findByStockIdAndReportDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.empty());

            AnalysisReportResponse aiResponse = sampleReport();
            when(aiAnalysisProvider.generateReport("005930")).thenReturn(aiResponse);

            AnalysisReportResponse result = analysisService.getReport("005930");

            assertEquals("005930", result.stockCode());
        }
    }

    @Nested
    @DisplayName("getHistory()")
    class HistoryTests {

        @Test
        @DisplayName("Returns mapped report list from DB")
        void returnsHistory() {
            when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(samsungStock));

            StockAnalysisReportEntity report = new StockAnalysisReportEntity(
                    1L, samsungStock, LocalDate.now().minusDays(1),
                    "과거 분석", "설명", "[]", "{}",
                    "HOLD", "면책", LocalDateTime.now()
            );
            when(reportRepository.findByStockIdOrderByDateDesc(eq(1L), any()))
                    .thenReturn(List.of(report));

            List<AnalysisReportResponse> history = analysisService.getHistory("005930");

            assertEquals(1, history.size());
            assertEquals("과거 분석", history.get(0).summary());
        }

        @Test
        @DisplayName("Returns empty list when no history exists")
        void emptyHistory() {
            when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(samsungStock));
            when(reportRepository.findByStockIdOrderByDateDesc(eq(1L), any()))
                    .thenReturn(List.of());

            List<AnalysisReportResponse> history = analysisService.getHistory("005930");

            assertTrue(history.isEmpty());
        }
    }
}
