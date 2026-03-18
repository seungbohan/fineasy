package com.fineasy.service;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Static financial ontology mapping sectors to macro factors and their relationships.
 * Provides structured domain knowledge for AI analysis context.
 */
@Component
public class FinanceOntology {

    // Sector → sensitive macro factors
    private static final Map<String, SectorProfile> SECTOR_PROFILES = new LinkedHashMap<>();

    static {
        SECTOR_PROFILES.put("반도체", new SectorProfile(
                "반도체", "Semiconductor",
                List.of("US_FED_FUNDS_RATE", "USD_KRW", "US_10Y_TREASURY"),
                List.of("AI/HBM 수요 사이클", "미중 반도체 규제", "파운드리 경쟁", "메모리 가격 동향"),
                List.of("GEOPOLITICAL", "INDUSTRY"),
                "금리 인상 시 기술주 밸류에이션 하락 압력, 달러 강세 시 수출 기업 환차익 가능, AI 수요가 현재 핵심 성장 동력"
        ));

        SECTOR_PROFILES.put("IT", new SectorProfile(
                "IT/소프트웨어", "IT/Software",
                List.of("US_FED_FUNDS_RATE", "US_10Y_TREASURY", "US_VIX"),
                List.of("클라우드 성장률", "AI 투자 확대", "SaaS 구독 경제", "빅테크 실적"),
                List.of("FISCAL", "INDUSTRY"),
                "성장주 특성상 금리에 민감, VIX 상승 시 기술주 매도 압력, AI 테마가 현재 밸류에이션 프리미엄 지지"
        ));

        SECTOR_PROFILES.put("자동차", new SectorProfile(
                "자동차", "Automotive",
                List.of("USD_KRW", "WTI", "US_FED_FUNDS_RATE"),
                List.of("EV 전환 속도", "배터리 원가", "자율주행 규제", "글로벌 자동차 수요"),
                List.of("GEOPOLITICAL", "INDUSTRY"),
                "환율 민감도 높은 수출 산업, 유가 상승 시 EV 수요 증가 가능, 미중 무역갈등 시 관세 리스크"
        ));

        SECTOR_PROFILES.put("2차전지", new SectorProfile(
                "2차전지/배터리", "Battery/EV",
                List.of("US_FED_FUNDS_RATE", "USD_KRW", "WTI"),
                List.of("리튬/니켈 원자재 가격", "IRA 보조금 정책", "EV 판매량", "전고체 배터리 개발"),
                List.of("FISCAL", "INDUSTRY"),
                "IRA 정책 수혜주, 원자재 가격에 마진 민감, EV 보급률이 핵심 성장 지표, 미국 정치 리스크에 민감"
        ));

        SECTOR_PROFILES.put("바이오", new SectorProfile(
                "바이오/제약", "Bio/Pharma",
                List.of("US_FED_FUNDS_RATE", "USD_KRW"),
                List.of("FDA 승인", "임상 결과", "신약 파이프라인", "바이오시밀러 시장"),
                List.of("INDUSTRY"),
                "임상/FDA 이벤트에 주가 극단적 반응, 금리 영향 상대적 적음, 개별 종목 이벤트 드리븐"
        ));

        SECTOR_PROFILES.put("금융", new SectorProfile(
                "금융/은행", "Financials/Banking",
                List.of("KR_BASE_RATE", "US_FED_FUNDS_RATE", "US_YIELD_SPREAD"),
                List.of("순이자마진(NIM)", "가계부채", "부동산 경기", "금융규제"),
                List.of("FISCAL"),
                "금리 상승 시 NIM 확대로 수혜, 장단기 금리차 확대 시 긍정적, 부동산 침체 시 부실 우려"
        ));

        SECTOR_PROFILES.put("에너지", new SectorProfile(
                "에너지/유틸리티", "Energy/Utilities",
                List.of("WTI", "GOLD", "US_DXY"),
                List.of("OPEC 감산", "중동 지정학 리스크", "신재생에너지 정책", "탄소중립"),
                List.of("GEOPOLITICAL", "BLACK_SWAN"),
                "유가 직접 연동, 중동 긴장 시 수혜, 달러 약세 시 원자재 가격 상승, 에너지 전환 정책 모니터링 필요"
        ));

        SECTOR_PROFILES.put("소비재", new SectorProfile(
                "소비재/유통", "Consumer/Retail",
                List.of("KR_BASE_RATE", "USD_KRW", "US_VIX"),
                List.of("소비심리지수", "물가상승률", "온라인 쇼핑 비중", "인구 구조 변화"),
                List.of("FISCAL"),
                "내수 경기에 민감, 금리 인상 시 소비 위축, 인플레이션 지속 시 마진 압박"
        ));

        SECTOR_PROFILES.put("철강", new SectorProfile(
                "철강/소재", "Steel/Materials",
                List.of("USD_KRW", "US_DXY", "WTI"),
                List.of("중국 경기부양책", "인프라 투자", "원자재 슈퍼사이클", "공급 과잉"),
                List.of("GEOPOLITICAL", "FISCAL"),
                "중국 수요가 핵심 변수, 달러 약세 시 원자재 가격 상승, 글로벌 인프라 투자 확대 시 수혜"
        ));

        SECTOR_PROFILES.put("통신", new SectorProfile(
                "통신/미디어", "Telecom/Media",
                List.of("KR_BASE_RATE"),
                List.of("5G/6G 투자", "OTT 경쟁", "배당 정책", "AI 인프라 수요"),
                List.of("INDUSTRY"),
                "배당주 성격으로 금리 영향 상대적 적음, 안정적 캐시플로우, AI 데이터센터 수요로 새로운 성장 동력"
        ));
    }

    // Well-known overseas stock → sector mapping
    private static final Map<String, String> OVERSEAS_STOCK_SECTORS = Map.ofEntries(
            Map.entry("AAPL", "IT"), Map.entry("MSFT", "IT"), Map.entry("GOOGL", "IT"),
            Map.entry("AMZN", "IT"), Map.entry("META", "IT"), Map.entry("NFLX", "IT"),
            Map.entry("NVDA", "반도체"), Map.entry("AMD", "반도체"), Map.entry("INTC", "반도체"),
            Map.entry("AVGO", "반도체"), Map.entry("QCOM", "반도체"), Map.entry("TSM", "반도체"),
            Map.entry("TSLA", "자동차"), Map.entry("F", "자동차"), Map.entry("GM", "자동차"),
            Map.entry("JPM", "금융"), Map.entry("BAC", "금융"), Map.entry("GS", "금융"),
            Map.entry("XOM", "에너지"), Map.entry("CVX", "에너지"), Map.entry("COP", "에너지"),
            Map.entry("JNJ", "바이오"), Map.entry("PFE", "바이오"), Map.entry("LLY", "바이오"),
            Map.entry("WMT", "소비재"), Map.entry("COST", "소비재"), Map.entry("TGT", "소비재"),
            Map.entry("T", "통신"), Map.entry("VZ", "통신")
    );

    /**
     * Get sector profile for a stock based on its sector or code.
     */
    public SectorProfile getSectorProfile(String stockCode, String sector) {
        // Try overseas stock mapping first
        String mappedSector = OVERSEAS_STOCK_SECTORS.get(stockCode);
        if (mappedSector != null && SECTOR_PROFILES.containsKey(mappedSector)) {
            return SECTOR_PROFILES.get(mappedSector);
        }

        // Try sector name matching
        if (sector != null) {
            for (Map.Entry<String, SectorProfile> entry : SECTOR_PROFILES.entrySet()) {
                if (sector.contains(entry.getKey()) || entry.getKey().contains(sector)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    /**
     * Build ontology context string for AI prompts.
     */
    public String buildOntologyContext(String stockCode, String sector) {
        SectorProfile profile = getSectorProfile(stockCode, sector);
        if (profile == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n### 섹터 도메인 지식 (온톨로지)\n");
        sb.append(String.format("- 섹터: %s (%s)\n", profile.nameKo(), profile.nameEn()));
        sb.append(String.format("- 핵심 민감 지표: %s\n", String.join(", ", profile.sensitiveIndicators())));
        sb.append(String.format("- 주요 산업 이슈: %s\n", String.join(", ", profile.keyIndustryFactors())));
        sb.append(String.format("- 관련 이벤트 유형: %s\n", String.join(", ", profile.relevantEventTypes())));
        sb.append(String.format("- 분석 가이드: %s\n", profile.analysisGuide()));

        return sb.toString();
    }

    public record SectorProfile(
            String nameKo,
            String nameEn,
            List<String> sensitiveIndicators,
            List<String> keyIndustryFactors,
            List<String> relevantEventTypes,
            String analysisGuide
    ) {}
}
