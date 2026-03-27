package com.fineasy.service;

import com.fineasy.entity.Sentiment;
import com.fineasy.service.KeywordSentimentAnalyzer.SentimentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeywordSentimentAnalyzerTest {

    private KeywordSentimentAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new KeywordSentimentAnalyzer();
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Null title returns NEUTRAL with 0.5 score")
        void nullTitle() {
            SentimentResult result = analyzer.analyze(null);

            assertEquals(Sentiment.NEUTRAL, result.sentiment());
            assertEquals(0.5, result.score());
        }

        @Test
        @DisplayName("Empty string returns NEUTRAL with 0.5 score")
        void emptyTitle() {
            SentimentResult result = analyzer.analyze("");

            assertEquals(Sentiment.NEUTRAL, result.sentiment());
            assertEquals(0.5, result.score());
        }

        @Test
        @DisplayName("Blank string returns NEUTRAL with 0.5 score")
        void blankTitle() {
            SentimentResult result = analyzer.analyze("   ");

            assertEquals(Sentiment.NEUTRAL, result.sentiment());
            assertEquals(0.5, result.score());
        }

        @Test
        @DisplayName("Title with no keywords returns NEUTRAL with 0.5 score")
        void noKeywordsFound() {
            SentimentResult result = analyzer.analyze("오늘 날씨가 좋습니다");

            assertEquals(Sentiment.NEUTRAL, result.sentiment());
            assertEquals(0.5, result.score());
        }
    }

    @Nested
    @DisplayName("Korean positive keywords")
    class KoreanPositive {

        @Test
        @DisplayName("Single positive keyword yields POSITIVE")
        void singlePositive() {
            SentimentResult result = analyzer.analyze("삼성전자 주가 급등 전망");

            assertEquals(Sentiment.POSITIVE, result.sentiment());
            assertTrue(result.score() > 0.6);
        }

        @Test
        @DisplayName("Multiple positive keywords yield high score")
        void multiplePositive() {
            SentimentResult result = analyzer.analyze("삼성전자 상승 호실적 역대최고 성장");

            assertEquals(Sentiment.POSITIVE, result.sentiment());
            assertTrue(result.score() > 0.8, "Score should be high with many positive keywords");
        }
    }

    @Nested
    @DisplayName("Korean negative keywords")
    class KoreanNegative {

        @Test
        @DisplayName("Single negative keyword yields NEGATIVE")
        void singleNegative() {
            SentimentResult result = analyzer.analyze("반도체 주가 급락 우려");

            assertEquals(Sentiment.NEGATIVE, result.sentiment());
            assertTrue(result.score() < 0.4);
        }

        @Test
        @DisplayName("Multiple negative keywords yield low score")
        void multipleNegative() {
            SentimentResult result = analyzer.analyze("적자 하락 위기 폭락 손실");

            assertEquals(Sentiment.NEGATIVE, result.sentiment());
            assertTrue(result.score() < 0.2, "Score should be low with many negative keywords");
        }
    }

    @Nested
    @DisplayName("English keywords")
    class EnglishKeywords {

        @Test
        @DisplayName("English positive keyword yields POSITIVE")
        void englishPositive() {
            SentimentResult result = analyzer.analyze("Tesla stock surge after earnings beat");

            assertEquals(Sentiment.POSITIVE, result.sentiment());
            assertTrue(result.score() > 0.6);
        }

        @Test
        @DisplayName("English negative keyword yields NEGATIVE")
        void englishNegative() {
            SentimentResult result = analyzer.analyze("Market crash fears as recession looms");

            assertEquals(Sentiment.NEGATIVE, result.sentiment());
            assertTrue(result.score() < 0.4);
        }

        @Test
        @DisplayName("English keywords are case-insensitive")
        void caseInsensitive() {
            SentimentResult resultLower = analyzer.analyze("stock surge");
            SentimentResult resultUpper = analyzer.analyze("Stock SURGE");

            assertEquals(resultLower.sentiment(), resultUpper.sentiment());
        }
    }

    @Nested
    @DisplayName("Mixed sentiment")
    class MixedSentiment {

        @Test
        @DisplayName("Equal-weight positive and negative keywords yield NEUTRAL")
        void balancedSentiment() {
            // 상승(1.0) + 호조(1.0) vs 하락(1.0) + 약세(1.0) = balanced
            SentimentResult result = analyzer.analyze("상승 호조이지만 하락 약세도 있다");

            assertEquals(Sentiment.NEUTRAL, result.sentiment());
            assertEquals(0.5, result.score(), 0.01);
        }

        @Test
        @DisplayName("Mixed Korean and English keywords both counted")
        void mixedLanguage() {
            SentimentResult result = analyzer.analyze("삼성전자 상승 rally strong 성장");

            assertEquals(Sentiment.POSITIVE, result.sentiment());
            assertTrue(result.score() > 0.6);
        }
    }

    @Nested
    @DisplayName("Score boundaries")
    class ScoreBoundaries {

        @Test
        @DisplayName("Score is always between 0.0 and 1.0")
        void scoreBounds() {
            SentimentResult allPositive = analyzer.analyze(
                    "상승 급등 호실적 매수 역대최고 수혜 성장 흑자 배당 확대 신기록 돌파 강세");
            assertTrue(allPositive.score() >= 0.0 && allPositive.score() <= 1.0);

            SentimentResult allNegative = analyzer.analyze(
                    "하락 급락 적자 매도 역대최저 악재 부진 우려 감소 손실 위기 약세");
            assertTrue(allNegative.score() >= 0.0 && allNegative.score() <= 1.0);
        }

        @Test
        @DisplayName("All positive keywords yield score of 1.0")
        void allPositiveScore() {
            SentimentResult result = analyzer.analyze("상승 급등 호실적");
            assertEquals(1.0, result.score(), 0.01);
        }

        @Test
        @DisplayName("All negative keywords yield score of 0.0")
        void allNegativeScore() {
            SentimentResult result = analyzer.analyze("하락 급락 적자");
            assertEquals(0.0, result.score(), 0.01);
        }
    }
}
