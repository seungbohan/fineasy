package com.fineasy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.config.OpenAiConfig;
import com.fineasy.entity.Market;
import com.fineasy.entity.NewsArticleEntity;
import com.fineasy.entity.Sentiment;
import com.fineasy.entity.StockEntity;
import com.fineasy.external.openai.OpenAiClient;
import com.fineasy.external.openai.OpenAiPromptBuilder;
import com.fineasy.repository.NewsArticleRepository;
import com.fineasy.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsSentimentServiceTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private OpenAiPromptBuilder promptBuilder;

    @Mock
    private OpenAiConfig openAiConfig;

    @Mock
    private KeywordSentimentAnalyzer keywordSentimentAnalyzer;

    private NewsSentimentService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new NewsSentimentService(
                newsArticleRepository, stockRepository,
                openAiClient, promptBuilder, objectMapper,
                openAiConfig, keywordSentimentAnalyzer
        );
    }

    private NewsArticleEntity createArticle(String title) {
        return new NewsArticleEntity(
                null, title, null,
                "https://example.com/" + title.hashCode(),
                "테스트뉴스", LocalDateTime.now(),
                null, null, LocalDateTime.now(),
                new ArrayList<>()
        );
    }

    private StockEntity createStock(String code, String name) {
        return new StockEntity(
                (long) code.hashCode(), code, name,
                Market.KRX, "반도체", true, LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("analyzeSentiment() - Empty input")
    class EmptyInput {

        @Test
        @DisplayName("Does nothing when article list is empty")
        void emptyListNoOp() {
            service.analyzeSentiment(List.of());

            verifyNoInteractions(openAiClient);
            verifyNoInteractions(newsArticleRepository);
            verifyNoInteractions(keywordSentimentAnalyzer);
        }
    }

    @Nested
    @DisplayName("analyzeSentiment() - Keyword fallback (OpenAI unavailable)")
    class KeywordFallback {

        @BeforeEach
        void setUpNoAi() {
            when(openAiConfig.getApiKey()).thenReturn("");
        }

        @Test
        @DisplayName("Uses keyword analyzer when OpenAI key is empty")
        void usesKeywordAnalyzer() {
            StockEntity samsung = createStock("005930", "삼성전자");
            when(stockRepository.findAll()).thenReturn(List.of(samsung));
            when(keywordSentimentAnalyzer.analyze(anyString()))
                    .thenReturn(new KeywordSentimentAnalyzer.SentimentResult(Sentiment.POSITIVE, 0.8));

            NewsArticleEntity article = createArticle("삼성전자 주가 급등");
            service.analyzeSentiment(new ArrayList<>(List.of(article)));

            verify(keywordSentimentAnalyzer).analyze("삼성전자 주가 급등");
            assertEquals(Sentiment.POSITIVE, article.getSentiment());
            assertEquals(0.8, article.getSentimentScore(), 0.01);
        }

        @Test
        @DisplayName("Tags stocks by name match in title")
        void tagsStocksByName() {
            StockEntity samsung = createStock("005930", "삼성전자");
            when(stockRepository.findAll()).thenReturn(List.of(samsung));
            when(keywordSentimentAnalyzer.analyze(anyString()))
                    .thenReturn(new KeywordSentimentAnalyzer.SentimentResult(Sentiment.NEUTRAL, 0.5));

            NewsArticleEntity article = createArticle("삼성전자 실적 발표");
            service.analyzeSentiment(new ArrayList<>(List.of(article)));

            assertTrue(article.getTaggedStocks().contains(samsung),
                    "Article mentioning '삼성전자' should be tagged with Samsung stock");
        }

        @Test
        @DisplayName("Keeps articles even with no stock tags in keyword fallback mode")
        void keepsUntaggedArticlesInFallback() {
            when(stockRepository.findAll()).thenReturn(List.of());
            when(keywordSentimentAnalyzer.analyze(anyString()))
                    .thenReturn(new KeywordSentimentAnalyzer.SentimentResult(Sentiment.NEUTRAL, 0.5));

            NewsArticleEntity article = createArticle("날씨 관련 뉴스");
            service.analyzeSentiment(new ArrayList<>(List.of(article)));

            verify(newsArticleRepository).saveAll(any());
            verify(newsArticleRepository, never()).deleteAll(any());
        }
    }

    @Nested
    @DisplayName("analyzeSentiment() - AI-powered")
    class AiPowered {

        @BeforeEach
        void setUpAi() {
            when(openAiConfig.getApiKey()).thenReturn("sk-test-key");
        }

        @Test
        @DisplayName("Processes batch with AI and parses sentiment")
        void batchAiAnalysis() {
            StockEntity samsung = createStock("005930", "삼성전자");
            when(stockRepository.findAll()).thenReturn(List.of(samsung));
            when(promptBuilder.getSentimentSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildSentimentPrompt(any())).thenReturn("user");

            String aiResponse = """
                    {
                      "results": [
                        {"index": 1, "sentiment": "POSITIVE", "score": 0.9, "stockRelated": true, "stocks": ["삼성전자"]},
                        {"index": 2, "sentiment": "NEGATIVE", "score": 0.3, "stockRelated": true, "stocks": ["삼성전자"]}
                      ]
                    }
                    """;
            when(openAiClient.chat(anyString(), anyString())).thenReturn(aiResponse);

            NewsArticleEntity article1 = createArticle("삼성전자 호실적 발표");
            NewsArticleEntity article2 = createArticle("삼성전자 실적 부진 우려");

            service.analyzeSentiment(new ArrayList<>(List.of(article1, article2)));

            assertEquals(Sentiment.POSITIVE, article1.getSentiment());
            assertEquals(0.9, article1.getSentimentScore(), 0.01);
            assertEquals(Sentiment.NEGATIVE, article2.getSentiment());
            assertTrue(article1.getTaggedStocks().contains(samsung));
        }

        @Test
        @DisplayName("Marks non-stock-related articles for deletion")
        void deletesNonStockRelated() {
            when(stockRepository.findAll()).thenReturn(List.of());
            when(promptBuilder.getSentimentSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildSentimentPrompt(any())).thenReturn("user");

            String aiResponse = """
                    {
                      "results": [
                        {"index": 1, "sentiment": "NEUTRAL", "score": 0.5, "stockRelated": false, "stocks": []}
                      ]
                    }
                    """;
            when(openAiClient.chat(anyString(), anyString())).thenReturn(aiResponse);

            NewsArticleEntity article = createArticle("오늘의 날씨 전망");
            service.analyzeSentiment(new ArrayList<>(List.of(article)));

            verify(newsArticleRepository).deleteAll(any());
        }

        @Test
        @DisplayName("Falls back to keywords when AI call throws exception")
        void fallsBackOnAiFailure() {
            StockEntity samsung = createStock("005930", "삼성전자");
            when(stockRepository.findAll()).thenReturn(List.of(samsung));
            when(promptBuilder.getSentimentSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildSentimentPrompt(any())).thenReturn("user");
            when(openAiClient.chat(anyString(), anyString()))
                    .thenThrow(new RuntimeException("OpenAI API timeout"));
            when(keywordSentimentAnalyzer.analyze(anyString()))
                    .thenReturn(new KeywordSentimentAnalyzer.SentimentResult(Sentiment.POSITIVE, 0.7));

            NewsArticleEntity article = createArticle("삼성전자 상승 전망");
            service.analyzeSentiment(new ArrayList<>(List.of(article)));

            verify(keywordSentimentAnalyzer).analyze("삼성전자 상승 전망");
            assertEquals(Sentiment.POSITIVE, article.getSentiment());
        }

        @Test
        @DisplayName("Handles malformed AI response gracefully")
        void handlesMalformedResponse() {
            StockEntity samsung = createStock("005930", "삼성전자");
            when(stockRepository.findAll()).thenReturn(List.of(samsung));
            when(promptBuilder.getSentimentSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildSentimentPrompt(any())).thenReturn("user");
            when(openAiClient.chat(anyString(), anyString())).thenReturn("not valid json {{{");
            lenient().when(keywordSentimentAnalyzer.analyze(anyString()))
                    .thenReturn(new KeywordSentimentAnalyzer.SentimentResult(Sentiment.NEUTRAL, 0.5));

            NewsArticleEntity article = createArticle("삼성전자 뉴스");

            assertDoesNotThrow(() ->
                    service.analyzeSentiment(new ArrayList<>(List.of(article))));

            assertEquals(Sentiment.NEUTRAL, article.getSentiment());
        }

        @Test
        @DisplayName("Batches articles in groups of 5")
        void batchesIn5s() {
            when(stockRepository.findAll()).thenReturn(List.of());
            when(promptBuilder.getSentimentSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildSentimentPrompt(any())).thenReturn("user");

            String emptyResponse = "{\"results\": []}";
            when(openAiClient.chat(anyString(), anyString())).thenReturn(emptyResponse);

            List<NewsArticleEntity> articles = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                articles.add(createArticle("뉴스 " + i));
            }

            service.analyzeSentiment(articles);

            verify(openAiClient, times(2)).chat(anyString(), anyString());
        }
    }
}
