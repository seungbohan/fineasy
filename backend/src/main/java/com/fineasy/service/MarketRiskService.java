package com.fineasy.service;

import com.fineasy.dto.response.MarketRiskResponse;
import com.fineasy.dto.response.MarketRiskResponse.RiskIndicator;
import com.fineasy.entity.MacroIndicatorEntity;
import com.fineasy.repository.MacroIndicatorRepository;
import com.fineasy.util.ChangeCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MarketRiskService {

    private static final Logger log = LoggerFactory.getLogger(MarketRiskService.class);

    private static final List<String> RISK_INDICATOR_CODES = List.of(
            "US_VIX", "US_10Y_TREASURY", "US_2Y_TREASURY",
            "US_YIELD_SPREAD", "US_DXY", "US_CREDIT_SPREAD"
    );

    private final MacroIndicatorRepository macroRepo;

    public MarketRiskService(MacroIndicatorRepository macroRepo) {
        this.macroRepo = macroRepo;
    }

    @Cacheable(value = "market-risk-summary", unless = "#result == null")
    public MarketRiskResponse getRiskSummary() {
        Map<String, Double> previousValueMap = buildPreviousValueMap();

        List<RiskIndicator> indicators = new ArrayList<>();
        int totalScore = 0;
        int evaluatedCount = 0;

        for (String code : RISK_INDICATOR_CODES) {
            Optional<MacroIndicatorEntity> latestOpt = macroRepo.findLatestByCode(code);
            if (latestOpt.isEmpty()) {
                log.debug("No data available for risk indicator: {}", code);
                continue;
            }

            MacroIndicatorEntity latest = latestOpt.get();
            Double previousValue = previousValueMap.get(code);
            ChangeCalculator.Change change = ChangeCalculator.calculate(latest.getValue(), previousValue);

            RiskAssessment assessment = assessRisk(code, latest.getValue());

            indicators.add(new RiskIndicator(
                    code,
                    latest.getIndicatorName(),
                    latest.getValue(),
                    latest.getUnit(),
                    change.changeAmount(),
                    change.changeRate(),
                    assessment.level(),
                    assessment.description(),
                    latest.getRecordDate()
            ));

            totalScore += assessment.score();
            evaluatedCount++;
        }

        int overallScore = evaluatedCount > 0 ? totalScore / evaluatedCount : 0;
        String overallLevel = scoreToLevel(overallScore);
        String riskComment = generateRiskComment(overallLevel, indicators);
        String yieldCurveStatus = evaluateYieldCurve(indicators);

        return new MarketRiskResponse(
                overallLevel,
                overallScore,
                riskComment,
                indicators,
                yieldCurveStatus,
                LocalDate.now()
        );
    }

    private RiskAssessment assessRisk(String code, double value) {
        return switch (code) {
            case "US_VIX" -> assessVix(value);
            case "US_YIELD_SPREAD" -> assessYieldSpread(value);
            case "US_CREDIT_SPREAD" -> assessCreditSpread(value);
            case "US_DXY" -> assessDxy(value);
            case "US_10Y_TREASURY" -> assessTreasury10Y(value);
            case "US_2Y_TREASURY" -> assessTreasury2Y(value);
            default -> new RiskAssessment("MODERATE", 50, "평가 기준 미정의");
        };
    }

    private RiskAssessment assessVix(double value) {
        if (value < 15) {
            return new RiskAssessment("LOW", 10,
                    "시장 안정 구간. 투자심리 양호");
        } else if (value < 20) {
            return new RiskAssessment("LOW", 25,
                    "정상 범위. 시장 변동성 낮음");
        } else if (value < 30) {
            return new RiskAssessment("MODERATE", 50,
                    "변동성 상승 구간. 시장 불안감 증가");
        } else if (value < 40) {
            return new RiskAssessment("HIGH", 75,
                    "높은 변동성. 시장 공포 확산 주의");
        } else {
            return new RiskAssessment("EXTREME", 95,
                    "극심한 공포. 패닉셀 위험 경보");
        }
    }

    private RiskAssessment assessYieldSpread(double value) {
        if (value > 1.0) {
            return new RiskAssessment("LOW", 10,
                    "정상적 수익률 곡선. 경기 확장 신호");
        } else if (value > 0.5) {
            return new RiskAssessment("LOW", 25,
                    "양호한 수익률 곡선");
        } else if (value > 0) {
            return new RiskAssessment("MODERATE", 50,
                    "수익률 곡선 평탄화. 경기 둔화 가능성");
        } else if (value > -0.5) {
            return new RiskAssessment("HIGH", 80,
                    "수익률 곡선 역전. 경기침체 경고 신호");
        } else {
            return new RiskAssessment("EXTREME", 95,
                    "심각한 역전. 역사적으로 경기침체 선행 지표");
        }
    }

    private RiskAssessment assessCreditSpread(double value) {
        if (value < 3) {
            return new RiskAssessment("LOW", 15,
                    "신용 위험 낮음. 투자 심리 양호");
        } else if (value < 5) {
            return new RiskAssessment("MODERATE", 45,
                    "보통 수준의 신용 위험");
        } else if (value < 7) {
            return new RiskAssessment("HIGH", 70,
                    "신용 위험 상승. 기업 부도 위험 주의");
        } else {
            return new RiskAssessment("EXTREME", 90,
                    "극심한 신용 경색. 금융위기 수준");
        }
    }

    private RiskAssessment assessDxy(double value) {
        if (value < 95) {
            return new RiskAssessment("LOW", 20,
                    "달러 약세. 신흥국/원자재에 우호적");
        } else if (value < 105) {
            return new RiskAssessment("MODERATE", 40,
                    "달러 보통. 중립적 환경");
        } else if (value < 115) {
            return new RiskAssessment("HIGH", 65,
                    "달러 강세. 신흥국 자금 유출 우려");
        } else {
            return new RiskAssessment("EXTREME", 85,
                    "극단적 달러 강세. 글로벌 유동성 위축");
        }
    }

    private RiskAssessment assessTreasury10Y(double value) {
        if (value < 3.0) {
            return new RiskAssessment("LOW", 20,
                    "저금리 환경. 주식에 우호적");
        } else if (value < 4.0) {
            return new RiskAssessment("MODERATE", 40,
                    "보통 수준의 금리. 중립적");
        } else if (value < 5.0) {
            return new RiskAssessment("HIGH", 65,
                    "고금리. 기업 차입비용 증가, 주식 밸류에이션 부담");
        } else {
            return new RiskAssessment("EXTREME", 85,
                    "매우 높은 금리. 경기 위축 및 자산 가격 하락 압력");
        }
    }

    private RiskAssessment assessTreasury2Y(double value) {
        if (value < 3.0) {
            return new RiskAssessment("LOW", 20,
                    "연준 완화적 통화정책 기대");
        } else if (value < 4.0) {
            return new RiskAssessment("MODERATE", 40,
                    "중립적 금리 기대");
        } else if (value < 5.0) {
            return new RiskAssessment("HIGH", 65,
                    "연준 긴축적 통화정책 반영. 단기 유동성 부담");
        } else {
            return new RiskAssessment("EXTREME", 85,
                    "극단적 긴축 금리. 경기 침체 위험 증가");
        }
    }

    private String evaluateYieldCurve(List<RiskIndicator> indicators) {
        return indicators.stream()
                .filter(i -> "US_YIELD_SPREAD".equals(i.code()))
                .findFirst()
                .map(i -> {
                    if (i.value() > 0.25) return "NORMAL";
                    if (i.value() > -0.1) return "FLAT";
                    return "INVERTED";
                })
                .orElse("UNKNOWN");
    }

    private String scoreToLevel(int score) {
        if (score < 25) return "LOW";
        if (score < 50) return "MODERATE";
        if (score < 75) return "HIGH";
        return "EXTREME";
    }

    private String generateRiskComment(String level, List<RiskIndicator> indicators) {
        int availableCount = indicators.size();
        if (availableCount == 0) {
            return "위험 지표 데이터가 아직 수집되지 않았습니다.";
        }

        return switch (level) {
            case "LOW" -> "시장 전반적으로 안정적인 환경입니다. "
                    + "투자심리가 양호하며 위험 요인이 제한적입니다.";
            case "MODERATE" -> "일부 위험 지표에서 경계 신호가 감지됩니다. "
                    + "포트폴리오 리밸런싱을 고려해 볼 시점입니다.";
            case "HIGH" -> "다수의 위험 지표가 경고 수준입니다. "
                    + "방어적 투자 전략과 현금 비중 확대를 권장합니다.";
            case "EXTREME" -> "시장이 극심한 스트레스 상태입니다. "
                    + "위험 자산 비중 축소와 안전자산 선호를 강력히 권장합니다.";
            default -> "위험 수준을 평가 중입니다.";
        };
    }

    private Map<String, Double> buildPreviousValueMap() {
        try {
            return macroRepo.findPreviousIndicators().stream()
                    .filter(e -> RISK_INDICATOR_CODES.contains(e.getIndicatorCode()))
                    .collect(Collectors.toMap(
                            MacroIndicatorEntity::getIndicatorCode,
                            MacroIndicatorEntity::getValue,
                            (v1, v2) -> v1
                    ));
        } catch (Exception e) {
            log.warn("Failed to load previous risk indicator values: {}", e.getMessage());
            return Map.of();
        }
    }

    private record RiskAssessment(String level, int score, String description) {}
}
