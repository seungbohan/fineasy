package com.fineasy.service;

import com.fineasy.entity.DynamicOntologyEntity;
import com.fineasy.repository.DynamicOntologyRepository;
import org.springframework.stereotype.Component;

import com.fineasy.entity.MacroIndicatorEntity;
import java.time.LocalDate;
import java.util.*;

/**
 * Static financial ontology mapping sectors to macro factors and their relationships.
 * Provides structured domain knowledge for AI analysis context.
 */
@Component
public class FinanceOntology {

    private final DynamicOntologyRepository dynamicOntologyRepository;

    public FinanceOntology(DynamicOntologyRepository dynamicOntologyRepository) {
        this.dynamicOntologyRepository = dynamicOntologyRepository;
    }

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

        SECTOR_PROFILES.put("건설", new SectorProfile(
                "건설/인프라", "Construction/Infrastructure",
                List.of("KR_BASE_RATE", "USD_KRW"),
                List.of("부동산 PF 리스크", "SOC 예산", "해외 수주", "원자재 가격"),
                List.of("FISCAL", "GEOPOLITICAL"),
                "금리에 매우 민감(PF 대출), 정부 SOC 예산에 직접 영향, 중동 수주는 지정학 리스크와 연동"
        ));

        SECTOR_PROFILES.put("조선", new SectorProfile(
                "조선/해운", "Shipbuilding/Shipping",
                List.of("USD_KRW", "WTI", "US_DXY"),
                List.of("신규 수주 잔고", "LNG선 수요", "친환경 선박 규제", "해운 운임지수(BDI)"),
                List.of("GEOPOLITICAL", "INDUSTRY"),
                "달러 기반 계약으로 원화 약세 시 수혜, LNG선 슈퍼사이클, 중동/홍해 리스크 시 해운 운임 상승"
        ));

        SECTOR_PROFILES.put("게임", new SectorProfile(
                "게임/엔터테인먼트", "Gaming/Entertainment",
                List.of("USD_KRW", "US_VIX"),
                List.of("신작 출시 일정", "글로벌 매출 순위", "중국 판호", "IP 확장"),
                List.of("INDUSTRY"),
                "개별 종목 이벤트 드리븐(신작 성과), 중국 판호 정책 영향, 원화 약세 시 해외 매출 환차익"
        ));

        SECTOR_PROFILES.put("화장품", new SectorProfile(
                "화장품/뷰티", "Cosmetics/Beauty",
                List.of("USD_KRW", "US_VIX"),
                List.of("중국 소비 회복", "K-뷰티 트렌드", "인디브랜드 성장", "면세점 매출"),
                List.of("INDUSTRY", "GEOPOLITICAL"),
                "중국 소비 심리와 직접 연동, 한중관계 영향, K-뷰티 글로벌 확산으로 미국/일본 시장 성장"
        ));

        SECTOR_PROFILES.put("방산", new SectorProfile(
                "방위산업/항공", "Defense/Aerospace",
                List.of("USD_KRW", "US_VIX"),
                List.of("지정학적 긴장", "국방예산 증가", "수출 계약", "NATO 방위비 확대"),
                List.of("GEOPOLITICAL", "FISCAL"),
                "지정학적 긴장 고조 시 최대 수혜 섹터, 글로벌 국방비 증가 추세, K-방산 수출 확대"
        ));

        SECTOR_PROFILES.put("음식료", new SectorProfile(
                "음식료/식품", "Food/Beverage",
                List.of("KR_BASE_RATE", "WTI"),
                List.of("원재료 가격", "소비심리", "K-푸드 수출", "건강식 트렌드"),
                List.of("FISCAL"),
                "경기 방어주 성격, 원재료 가격 상승 시 마진 압박, 해외 진출 기업은 환율 영향"
        ));

        SECTOR_PROFILES.put("제약", new SectorProfile(
                "제약/헬스케어", "Pharma/Healthcare",
                List.of("US_FED_FUNDS_RATE", "USD_KRW"),
                List.of("비만치료제 시장", "ADC 항암제", "바이오시밀러", "CDMO 수요"),
                List.of("INDUSTRY"),
                "글로벌 바이오 트렌드(GLP-1, ADC)에 민감, 기술이전/라이선스 계약이 주가 촉매"
        ));
    }

    // Well-known overseas stock → sector mapping
    private static final Map<String, String> OVERSEAS_STOCK_SECTORS;
    static {
        Map<String, String> m = new HashMap<>();
        // IT / Software
        m.put("AAPL", "IT"); m.put("MSFT", "IT"); m.put("GOOGL", "IT"); m.put("GOOG", "IT");
        m.put("AMZN", "IT"); m.put("META", "IT"); m.put("NFLX", "IT"); m.put("CRM", "IT");
        m.put("ORCL", "IT"); m.put("ADBE", "IT"); m.put("NOW", "IT"); m.put("SNOW", "IT");
        m.put("PLTR", "IT"); m.put("UBER", "IT"); m.put("SHOP", "IT"); m.put("SQ", "IT");
        m.put("SPOT", "IT"); m.put("PINS", "IT"); m.put("SNAP", "IT"); m.put("RBLX", "IT");
        // Semiconductor
        m.put("NVDA", "반도체"); m.put("AMD", "반도체"); m.put("INTC", "반도체");
        m.put("AVGO", "반도체"); m.put("QCOM", "반도체"); m.put("TSM", "반도체");
        m.put("MU", "반도체"); m.put("MRVL", "반도체"); m.put("LRCX", "반도체");
        m.put("AMAT", "반도체"); m.put("KLAC", "반도체"); m.put("ASML", "반도체");
        m.put("ARM", "반도체"); m.put("ON", "반도체"); m.put("TXN", "반도체");
        m.put("SMCI", "반도체");
        // Automotive / EV
        m.put("TSLA", "자동차"); m.put("F", "자동차"); m.put("GM", "자동차");
        m.put("RIVN", "자동차"); m.put("LCID", "자동차"); m.put("NIO", "자동차");
        m.put("LI", "자동차"); m.put("XPEV", "자동차"); m.put("TM", "자동차");
        m.put("STLA", "자동차");
        // Financials
        m.put("JPM", "금융"); m.put("BAC", "금융"); m.put("GS", "금융");
        m.put("MS", "금융"); m.put("WFC", "금융"); m.put("C", "금융");
        m.put("BLK", "금융"); m.put("SCHW", "금융"); m.put("V", "금융");
        m.put("MA", "금융"); m.put("AXP", "금융"); m.put("PYPL", "금융");
        // Energy
        m.put("XOM", "에너지"); m.put("CVX", "에너지"); m.put("COP", "에너지");
        m.put("SLB", "에너지"); m.put("EOG", "에너지"); m.put("OXY", "에너지");
        m.put("MPC", "에너지"); m.put("PSX", "에너지");
        // Bio / Pharma
        m.put("JNJ", "바이오"); m.put("PFE", "바이오"); m.put("LLY", "바이오");
        m.put("ABBV", "바이오"); m.put("MRK", "바이오"); m.put("BMY", "바이오");
        m.put("AMGN", "바이오"); m.put("GILD", "바이오"); m.put("REGN", "바이오");
        m.put("MRNA", "바이오"); m.put("ISRG", "바이오"); m.put("TMO", "바이오");
        m.put("UNH", "바이오"); m.put("NVO", "제약"); m.put("AZN", "제약");
        // Consumer / Retail
        m.put("WMT", "소비재"); m.put("COST", "소비재"); m.put("TGT", "소비재");
        m.put("HD", "소비재"); m.put("LOW", "소비재"); m.put("NKE", "소비재");
        m.put("SBUX", "소비재"); m.put("MCD", "소비재"); m.put("KO", "소비재");
        m.put("PEP", "소비재"); m.put("PG", "소비재"); m.put("LULU", "소비재");
        // Telecom
        m.put("T", "통신"); m.put("VZ", "통신"); m.put("TMUS", "통신");
        m.put("CMCSA", "통신"); m.put("DIS", "통신");
        // Defense
        m.put("LMT", "방산"); m.put("RTX", "방산"); m.put("NOC", "방산");
        m.put("GD", "방산"); m.put("BA", "방산");
        // Steel / Materials
        m.put("NUE", "철강"); m.put("CLF", "철강"); m.put("X", "철강");
        m.put("FCX", "철강"); m.put("AA", "철강");
        // Battery / EV supply chain
        m.put("ALB", "2차전지"); m.put("PANW", "IT");
        // Gaming
        m.put("NTDOY", "게임"); m.put("ATVI", "게임"); m.put("EA", "게임");
        m.put("TTWO", "게임"); m.put("RBLX", "게임");

        OVERSEAS_STOCK_SECTORS = Map.copyOf(m);
    }

    // English sector name → Korean sector key fallback mapping (for DB sector fields)
    private static final Map<String, String> ENGLISH_SECTOR_MAP = Map.ofEntries(
            Map.entry("technology", "IT"), Map.entry("software", "IT"), Map.entry("internet", "IT"),
            Map.entry("semiconductor", "반도체"), Map.entry("semiconductors", "반도체"), Map.entry("chips", "반도체"),
            Map.entry("automotive", "자동차"), Map.entry("auto", "자동차"), Map.entry("electric vehicle", "자동차"),
            Map.entry("battery", "2차전지"), Map.entry("ev", "2차전지"), Map.entry("lithium", "2차전지"),
            Map.entry("healthcare", "바이오"), Map.entry("biotech", "바이오"),
            Map.entry("pharmaceutical", "바이오"), Map.entry("pharma", "바이오"), Map.entry("drug", "바이오"),
            Map.entry("financial", "금융"), Map.entry("financials", "금융"), Map.entry("banking", "금융"),
            Map.entry("insurance", "금융"), Map.entry("fintech", "금융"),
            Map.entry("energy", "에너지"), Map.entry("utilities", "에너지"), Map.entry("oil", "에너지"),
            Map.entry("consumer", "소비재"), Map.entry("retail", "소비재"), Map.entry("food", "음식료"),
            Map.entry("beverage", "음식료"), Map.entry("restaurant", "음식료"),
            Map.entry("steel", "철강"), Map.entry("materials", "철강"), Map.entry("mining", "철강"),
            Map.entry("telecom", "통신"), Map.entry("communication", "통신"), Map.entry("media", "통신"),
            Map.entry("construction", "건설"), Map.entry("infrastructure", "건설"), Map.entry("real estate", "건설"),
            Map.entry("shipbuilding", "조선"), Map.entry("shipping", "조선"), Map.entry("marine", "조선"),
            Map.entry("gaming", "게임"), Map.entry("game", "게임"), Map.entry("entertainment", "게임"),
            Map.entry("cosmetics", "화장품"), Map.entry("beauty", "화장품"),
            Map.entry("defense", "방산"), Map.entry("aerospace", "방산"), Map.entry("military", "방산")
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

        // Try sector name matching (Korean)
        if (sector != null) {
            for (Map.Entry<String, SectorProfile> entry : SECTOR_PROFILES.entrySet()) {
                if (sector.contains(entry.getKey()) || entry.getKey().contains(sector)) {
                    return entry.getValue();
                }
            }

            // Fallback: try English sector name mapping (for overseas stocks with DB sector)
            String englishMapped = ENGLISH_SECTOR_MAP.get(sector.toLowerCase().trim());
            if (englishMapped != null && SECTOR_PROFILES.containsKey(englishMapped)) {
                return SECTOR_PROFILES.get(englishMapped);
            }
        }

        return null;
    }

    /**
     * Get sector keywords for enriching semantic search queries.
     * Returns space-joined key industry factors, or null if sector is unknown.
     */
    public String getSectorKeywords(String stockCode, String sector) {
        SectorProfile profile = getSectorProfile(stockCode, sector);
        if (profile == null) {
            return null;
        }
        return String.join(" ", profile.keyIndustryFactors());
    }

    /**
     * Build ontology context string for AI prompts (backward compatible).
     */
    public String buildOntologyContext(String stockCode, String sector) {
        return buildOntologyContext(stockCode, sector, null);
    }

    /**
     * Build enriched ontology context with actual macro indicator values.
     * Links sector sensitivity to real-time macro data for concrete analysis guidance.
     */
    public String buildOntologyContext(String stockCode, String sector,
                                        List<MacroIndicatorEntity> macroIndicators) {
        StringBuilder sb = new StringBuilder();

        SectorProfile profile = getSectorProfile(stockCode, sector);
        if (profile != null) {
            sb.append("\n### 섹터 분석 프레임워크 (AI 가이드)\n");
            sb.append(String.format("섹터: %s (%s)\n", profile.nameKo(), profile.nameEn()));
            sb.append(String.format("분석 가이드: %s\n", profile.analysisGuide()));

            // Link sensitive indicators to actual values
            if (macroIndicators != null && !macroIndicators.isEmpty()) {
                sb.append("\n#### 이 섹터의 핵심 민감 지표 (현재 값)\n");
                Map<String, MacroIndicatorEntity> macroMap = new HashMap<>();
                for (MacroIndicatorEntity m : macroIndicators) {
                    macroMap.put(m.getIndicatorCode(), m);
                }

                for (String indicatorCode : profile.sensitiveIndicators()) {
                    MacroIndicatorEntity actual = macroMap.get(indicatorCode);
                    if (actual != null) {
                        String impact = assessMacroImpact(indicatorCode, actual.getValue(), profile);
                        sb.append(String.format("- %s: %.2f%s → %s\n",
                                actual.getIndicatorName() != null ? actual.getIndicatorName() : indicatorCode,
                                actual.getValue(),
                                actual.getUnit() != null ? actual.getUnit() : "",
                                impact));
                    } else {
                        sb.append(String.format("- %s: 데이터 없음\n", indicatorCode));
                    }
                }
            } else {
                sb.append(String.format("핵심 민감 지표: %s\n", String.join(", ", profile.sensitiveIndicators())));
            }

            sb.append(String.format("\n주요 산업 이슈: %s\n", String.join(", ", profile.keyIndustryFactors())));
            sb.append(String.format("관련 이벤트 유형: %s\n", String.join(", ", profile.relevantEventTypes())));
            sb.append("\n※ 위 프레임워크를 참고하되, 제공된 뉴스/데이터와 상충하면 실제 데이터를 우선하세요.\n");
        }

        appendDynamicOntology(sb, stockCode, sector);

        return sb.toString();
    }

    /**
     * Assess the directional impact of a macro indicator value on a sector.
     */
    private String assessMacroImpact(String indicatorCode, double value, SectorProfile profile) {
        return switch (indicatorCode) {
            case "US_FED_FUNDS_RATE", "KR_BASE_RATE" -> {
                if (value >= 5.0) yield "고금리 환경, " + profile.nameKo() + " 밸류에이션 압박 가능";
                else if (value >= 3.0) yield "중립적 금리 수준";
                else yield "저금리 환경, 성장주/위험자산 선호";
            }
            case "USD_KRW" -> {
                if (value >= 1400) yield "원화 약세, 수출기업 환차익 가능 / 수입원가 부담";
                else if (value >= 1250) yield "환율 중립 구간";
                else yield "원화 강세, 수출기업 환차손 / 내수주 상대적 유리";
            }
            case "US_VIX" -> {
                if (value >= 30) yield "시장 공포 구간, 위험자산 회피 심리 강화";
                else if (value >= 20) yield "불확실성 상승, 변동성 확대 주의";
                else yield "시장 안정 구간, 위험자산 선호";
            }
            case "WTI" -> {
                if (value >= 90) yield "고유가, 에너지 수혜 / 제조업 원가 부담";
                else if (value >= 70) yield "유가 중립 구간";
                else yield "저유가, 제조업 원가 절감 / 에너지 기업 수익성 악화";
            }
            case "US_10Y_TREASURY" -> {
                if (value >= 4.5) yield "장기금리 고점, 성장주 할인율 상승 압박";
                else if (value >= 3.5) yield "장기금리 중립";
                else yield "장기금리 하락, 성장주 밸류에이션 지지";
            }
            case "US_YIELD_SPREAD" -> {
                if (value < 0) yield "장단기 금리 역전, 경기침체 시그널";
                else if (value < 0.5) yield "스프레드 축소, 경기 둔화 우려";
                else yield "정상적 금리 구조, 금융주 NIM 지지";
            }
            case "GOLD" -> {
                if (value >= 2500) yield "금 가격 고점, 안전자산 선호 / 인플레이션 헤지 수요";
                else yield "금 가격 안정";
            }
            case "US_DXY" -> {
                if (value >= 105) yield "달러 강세, 원자재/신흥국 자산 약세 압력";
                else if (value >= 100) yield "달러 중립";
                else yield "달러 약세, 원자재 가격 지지 / 신흥국 자산 유리";
            }
            default -> "이 섹터에 영향을 미치는 지표";
        };
    }

    private void appendDynamicOntology(StringBuilder sb, String stockCode, String sector) {
        try {
            List<DynamicOntologyEntity> entries =
                    dynamicOntologyRepository.findValidBySectorOrStock(
                            sector, stockCode, LocalDate.now());

            if (entries.isEmpty()) return;

            sb.append("\n### 최근 시장 동향 (자동 분석, 최근 2주)\n");

            for (DynamicOntologyEntity entry : entries) {
                switch (entry.getEntryType()) {
                    case "HOT_ISSUE" -> sb.append(String.format(
                            "- [핫이슈] %s: %s (관련: %s)\n",
                            entry.getSubject(),
                            entry.getDescription(),
                            entry.getRelatedStocks() != null ? entry.getRelatedStocks() : ""));
                    case "STOCK_RELATION" -> sb.append(String.format(
                            "- [종목관계] %s: %s\n",
                            entry.getSubject(), entry.getDescription()));
                    case "EMERGING_THEME" -> sb.append(String.format(
                            "- [신규테마] %s: %s (관련: %s)\n",
                            entry.getSubject(),
                            entry.getDescription(),
                            entry.getRelatedStocks() != null ? entry.getRelatedStocks() : ""));
                }
            }
        } catch (Exception e) {
            // Dynamic ontology is optional — don't break analysis if it fails
        }
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
