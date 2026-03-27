package com.fineasy.service;

import com.fineasy.entity.Sentiment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KeywordSentimentAnalyzer {

    // Weighted keywords: higher weight = stronger signal
    private static final List<WeightedKeyword> POSITIVE_KR = List.of(
            // Strong positive (weight 2.0)
            new WeightedKeyword("급등", 2.0), new WeightedKeyword("역대최고", 2.0),
            new WeightedKeyword("신고가", 2.0), new WeightedKeyword("돌파", 2.0),
            new WeightedKeyword("대규모 수주", 2.0), new WeightedKeyword("사상최대", 2.0),
            new WeightedKeyword("흑자전환", 2.0), new WeightedKeyword("계약체결", 2.0),
            // Normal positive (weight 1.0)
            new WeightedKeyword("상승", 1.0), new WeightedKeyword("호실적", 1.0),
            new WeightedKeyword("매수", 1.0), new WeightedKeyword("수혜", 1.0),
            new WeightedKeyword("성장", 1.0), new WeightedKeyword("흑자", 1.0),
            new WeightedKeyword("배당", 1.0), new WeightedKeyword("확대", 1.0),
            new WeightedKeyword("강세", 1.0), new WeightedKeyword("호조", 1.0),
            new WeightedKeyword("기대감", 1.0), new WeightedKeyword("수출증가", 1.0),
            new WeightedKeyword("실적개선", 1.0), new WeightedKeyword("반등", 1.0),
            new WeightedKeyword("호재", 1.0), new WeightedKeyword("최대", 1.0),
            new WeightedKeyword("개선", 1.0), new WeightedKeyword("증가", 1.0),
            new WeightedKeyword("상향", 1.0), new WeightedKeyword("인수", 1.0),
            new WeightedKeyword("수주", 1.0), new WeightedKeyword("호황", 1.0),
            new WeightedKeyword("목표가 상향", 1.5), new WeightedKeyword("어닝서프라이즈", 1.5),
            new WeightedKeyword("시장 컨센서스 상회", 1.5), new WeightedKeyword("순매수", 1.0),
            new WeightedKeyword("외국인 매수", 1.0), new WeightedKeyword("기관 매수", 1.0),
            // Weak positive (weight 0.5)
            new WeightedKeyword("전망", 0.5), new WeightedKeyword("기대", 0.5),
            new WeightedKeyword("가능성", 0.5)
    );

    private static final List<WeightedKeyword> NEGATIVE_KR = List.of(
            // Strong negative (weight 2.0)
            new WeightedKeyword("급락", 2.0), new WeightedKeyword("폭락", 2.0),
            new WeightedKeyword("역대최저", 2.0), new WeightedKeyword("신저가", 2.0),
            new WeightedKeyword("파산", 2.0), new WeightedKeyword("상장폐지", 2.0),
            new WeightedKeyword("적자전환", 2.0), new WeightedKeyword("대규모 손실", 2.0),
            // Normal negative (weight 1.0)
            new WeightedKeyword("하락", 1.0), new WeightedKeyword("적자", 1.0),
            new WeightedKeyword("매도", 1.0), new WeightedKeyword("악재", 1.0),
            new WeightedKeyword("리스크", 1.0), new WeightedKeyword("부진", 1.0),
            new WeightedKeyword("우려", 1.0), new WeightedKeyword("감소", 1.0),
            new WeightedKeyword("손실", 1.0), new WeightedKeyword("충격", 1.0),
            new WeightedKeyword("약세", 1.0), new WeightedKeyword("위기", 1.0),
            new WeightedKeyword("하회", 1.0), new WeightedKeyword("공포", 1.0),
            new WeightedKeyword("수출감소", 1.0), new WeightedKeyword("실적부진", 1.0),
            new WeightedKeyword("악화", 1.0), new WeightedKeyword("최저", 1.0),
            new WeightedKeyword("하향", 1.0), new WeightedKeyword("축소", 1.0),
            new WeightedKeyword("소송", 1.0), new WeightedKeyword("제재", 1.0),
            new WeightedKeyword("불안", 1.0), new WeightedKeyword("관세", 1.0),
            new WeightedKeyword("목표가 하향", 1.5), new WeightedKeyword("어닝쇼크", 1.5),
            new WeightedKeyword("시장 컨센서스 하회", 1.5), new WeightedKeyword("순매도", 1.0),
            new WeightedKeyword("외국인 매도", 1.0), new WeightedKeyword("기관 매도", 1.0),
            // Weak negative (weight 0.5)
            new WeightedKeyword("불확실", 0.5), new WeightedKeyword("변동성", 0.5)
    );

    private static final List<WeightedKeyword> POSITIVE_EN = List.of(
            new WeightedKeyword("surge", 2.0), new WeightedKeyword("soar", 2.0),
            new WeightedKeyword("record high", 2.0), new WeightedKeyword("all-time high", 2.0),
            new WeightedKeyword("rally", 1.0), new WeightedKeyword("gain", 1.0),
            new WeightedKeyword("bullish", 1.0), new WeightedKeyword("upgrade", 1.0),
            new WeightedKeyword("beat", 1.0), new WeightedKeyword("outperform", 1.0),
            new WeightedKeyword("growth", 1.0), new WeightedKeyword("profit", 1.0),
            new WeightedKeyword("breakout", 1.0), new WeightedKeyword("strong", 1.0),
            new WeightedKeyword("recovery", 1.0), new WeightedKeyword("boom", 1.0),
            new WeightedKeyword("buy", 1.0), new WeightedKeyword("upside", 1.0),
            new WeightedKeyword("optimism", 0.5)
    );

    private static final List<WeightedKeyword> NEGATIVE_EN = List.of(
            new WeightedKeyword("plunge", 2.0), new WeightedKeyword("crash", 2.0),
            new WeightedKeyword("record low", 2.0), new WeightedKeyword("collapse", 2.0),
            new WeightedKeyword("drop", 1.0), new WeightedKeyword("loss", 1.0),
            new WeightedKeyword("bearish", 1.0), new WeightedKeyword("downgrade", 1.0),
            new WeightedKeyword("miss", 1.0), new WeightedKeyword("underperform", 1.0),
            new WeightedKeyword("recession", 1.0), new WeightedKeyword("deficit", 1.0),
            new WeightedKeyword("weak", 1.0), new WeightedKeyword("slump", 1.0),
            new WeightedKeyword("sell", 1.0), new WeightedKeyword("downside", 1.0),
            new WeightedKeyword("fear", 1.0), new WeightedKeyword("tariff", 1.0),
            new WeightedKeyword("sanction", 1.0), new WeightedKeyword("pessimism", 0.5)
    );

    // Negation patterns that flip sentiment
    private static final List<String> NEGATION_KR = List.of(
            "해소", "완화", "극복", "회복", "벗어나", "탈출", "진정", "안정",
            "에도 불구", "불구하고", "전환", "반전"
    );

    private static final List<String> NEGATION_EN = List.of(
            "despite", "recover", "ease", "relief", "overcome", "rebound",
            "not ", "no longer", "turn around"
    );

    public SentimentResult analyze(String title) {
        if (title == null || title.isBlank()) {
            return new SentimentResult(Sentiment.NEUTRAL, 0.5);
        }

        String lowerTitle = title.toLowerCase();

        double positiveScore = calcWeightedScore(title, POSITIVE_KR)
                + calcWeightedScore(lowerTitle, POSITIVE_EN);
        double negativeScore = calcWeightedScore(title, NEGATIVE_KR)
                + calcWeightedScore(lowerTitle, NEGATIVE_EN);

        // Negation handling: "우려 해소", "리스크 완화" etc.
        boolean hasNegationKr = NEGATION_KR.stream().anyMatch(title::contains);
        boolean hasNegationEn = NEGATION_EN.stream().anyMatch(lowerTitle::contains);

        if (hasNegationKr || hasNegationEn) {
            // If negation + negative keywords: flip to positive (e.g., "우려 해소")
            if (negativeScore > positiveScore) {
                double temp = positiveScore;
                positiveScore = negativeScore * 0.7; // Slightly weaker than direct positive
                negativeScore = temp * 0.3;
            }
        }

        double totalScore = positiveScore + negativeScore;
        if (totalScore == 0) {
            return new SentimentResult(Sentiment.NEUTRAL, 0.5);
        }

        // Normalized score: 0.0 (very negative) to 1.0 (very positive)
        double score = (positiveScore - negativeScore + totalScore) / (2.0 * totalScore);
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

    private double calcWeightedScore(String text, List<WeightedKeyword> keywords) {
        double score = 0;
        for (WeightedKeyword kw : keywords) {
            if (text.contains(kw.keyword())) {
                score += kw.weight();
            }
        }
        return score;
    }

    private record WeightedKeyword(String keyword, double weight) {}

    public record SentimentResult(Sentiment sentiment, double score) {}
}
