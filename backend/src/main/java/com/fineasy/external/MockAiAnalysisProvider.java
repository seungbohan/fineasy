package com.fineasy.external;

import com.fineasy.dto.response.AnalysisReportResponse;
import com.fineasy.dto.response.PredictionResponse;
import com.fineasy.service.AiAnalysisProvider;
import com.fineasy.entity.PredictionDirection;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class MockAiAnalysisProvider implements AiAnalysisProvider {

    @Override
    public AnalysisReportResponse generateReport(String stockCode) {
        return new AnalysisReportResponse(
                stockCode,
                LocalDateTime.now(),
                "현재 이 종목은 단기 상승 신호를 보이고 있습니다.",
                "RSI 지표가 45로 중립 구간에 위치해 있으며, MACD가 골든크로스를 형성하고 있습니다. " +
                        "20일 이동평균선을 상향 돌파한 상태이며, 거래량이 평균 대비 120% 수준으로 " +
                        "매수세가 유입되고 있는 것으로 판단됩니다. 다만 볼린저 밴드 중심선 부근에 " +
                        "위치해 있어 추가 상승 여력은 제한적일 수 있습니다.",
                List.of(
                        "RSI 지표가 과매도 구간에서 회복 중",
                        "20일 이동평균선 상향 돌파",
                        "거래량이 평균 대비 120% 수준"
                ),
                "HOLD",
                "이 분석은 참고용이며 투자 권유가 아닙니다. 투자 결정은 본인의 판단과 책임 하에 이루어져야 합니다.",
                Map.of(
                        "rsi", Map.of("value", 45.2, "signal", "NEUTRAL"),
                        "macd", Map.of("value", 150, "signal", "BULLISH"),
                        "bollingerBand", Map.of("position", "MIDDLE", "signal", "NEUTRAL")
                )
        );
    }

    @Override
    public PredictionResponse generatePrediction(String stockCode, String period) {
        return new PredictionResponse(
                stockCode,
                period,
                PredictionDirection.UP,
                68,
                List.of(
                        "PER 12.5배로 업종 평균 대비 저평가",
                        "배당수익률 3.2%로 안정적 현금흐름",
                        "최근 뉴스 감성 긍정적 (0.72)",
                        "원/달러 환율 안정세로 수출 기업에 유리",
                        "거시경제 지표 안정적"
                ),
                "이 예측은 AI 모델 기반이며 투자 권유가 아닙니다. 실제 투자 시 전문가 상담을 권장합니다.",
                LocalDateTime.now(),
                "UNDERVALUED"
        );
    }
}
