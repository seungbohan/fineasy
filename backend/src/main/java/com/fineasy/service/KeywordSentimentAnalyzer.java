package com.fineasy.service;

import com.fineasy.entity.Sentiment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KeywordSentimentAnalyzer {

    private static final List<String> POSITIVE_KEYWORDS_KR = List.of(
            "상승", "급등", "호실적", "매수", "역대최고", "수혜", "계약체결",
            "성장", "흑자", "배당", "확대", "신기록", "돌파", "강세", "호조",
            "신고가", "기대감", "수출증가", "실적개선", "반등", "호재", "최대",
            "개선", "증가", "상향", "인수", "수주", "호황"
    );

    private static final List<String> NEGATIVE_KEYWORDS_KR = List.of(
            "하락", "급락", "적자", "매도", "역대최저", "악재", "리스크",
            "부진", "우려", "감소", "손실", "충격", "약세", "위기", "하회",
            "신저가", "공포", "수출감소", "실적부진", "폭락", "악화", "최저",
            "하향", "축소", "파산", "소송", "제재", "불안"
    );

    private static final List<String> POSITIVE_KEYWORDS_EN = List.of(
            "surge", "rally", "gain", "bullish", "record high", "upgrade",
            "beat", "outperform", "growth", "profit", "breakout", "strong",
            "recovery", "boom", "soar", "buy", "upside", "optimism"
    );

    private static final List<String> NEGATIVE_KEYWORDS_EN = List.of(
            "drop", "plunge", "loss", "bearish", "record low", "downgrade",
            "miss", "underperform", "recession", "deficit", "breakdown", "weak",
            "crash", "slump", "sell", "downside", "fear", "pessimism"
    );

    public SentimentResult analyze(String title) {
        if (title == null || title.isBlank()) {
            return new SentimentResult(Sentiment.NEUTRAL, 0.5);
        }

        String lowerTitle = title.toLowerCase();

        int positiveCount = countMatches(title, POSITIVE_KEYWORDS_KR)
                + countMatches(lowerTitle, POSITIVE_KEYWORDS_EN);
        int negativeCount = countMatches(title, NEGATIVE_KEYWORDS_KR)
                + countMatches(lowerTitle, NEGATIVE_KEYWORDS_EN);

        int totalKeywords = positiveCount + negativeCount;
        if (totalKeywords == 0) {
            return new SentimentResult(Sentiment.NEUTRAL, 0.5);
        }

        double score = (double) (positiveCount - negativeCount + totalKeywords) / (2.0 * totalKeywords);
        score = Math.max(0.0, Math.min(1.0, score));

        Sentiment sentiment;
        if (score > 0.6) {
            sentiment = Sentiment.POSITIVE;
        } else if (score < 0.4) {
            sentiment = Sentiment.NEGATIVE;
        } else {
            sentiment = Sentiment.NEUTRAL;
        }

        return new SentimentResult(sentiment, score);
    }

    private int countMatches(String text, List<String> keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                count++;
            }
        }
        return count;
    }

    public record SentimentResult(Sentiment sentiment, double score) {
    }
}
