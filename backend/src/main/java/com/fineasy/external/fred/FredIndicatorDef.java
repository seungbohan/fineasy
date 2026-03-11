package com.fineasy.external.fred;

public enum FredIndicatorDef {

    US_FED_FUNDS_RATE("US_FED_FUNDS_RATE", "EFFR", "D",
            "미국 연방기금금리", "%", "FRED", Category.POLICY),

    US_CPI("US_CPI", "CPIAUCSL", "M",
            "미국 소비자물가지수", "Index 1982-84=100", "FRED", Category.ECONOMY),

    US_PPI("US_PPI", "PPIACO", "M",
            "미국 생산자물가지수", "Index 1982=100", "FRED", Category.ECONOMY),

    US_PCE("US_PCE", "PCEPI", "M",
            "미국 개인소비지출 물가지수", "Index 2017=100", "FRED", Category.ECONOMY),

    US_UNEMPLOYMENT("US_UNEMPLOYMENT", "UNRATE", "M",
            "미국 실업률", "%", "FRED", Category.ECONOMY),

    US_NONFARM_PAYROLL("US_NONFARM_PAYROLL", "PAYEMS", "M",
            "미국 비농업고용", "천 명", "FRED", Category.ECONOMY),

    US_GDP("US_GDP", "GDP", "Q",
            "미국 GDP", "십억 달러", "FRED", Category.ECONOMY),

    US_RETAIL_SALES("US_RETAIL_SALES", "RSAFS", "M",
            "미국 소매판매", "백만 달러", "FRED", Category.ECONOMY),

    US_ISM_MFG("US_ISM_MFG", "MANEMP", "M",
            "미국 ISM 제조업 고용", "천 명", "FRED", Category.ECONOMY),

    US_10Y_TREASURY("US_10Y_TREASURY", "DGS10", "D",
            "미국 10년물 국채수익률", "%", "FRED", Category.FINANCIAL_MARKET),

    US_2Y_TREASURY("US_2Y_TREASURY", "DGS2", "D",
            "미국 2년물 국채수익률", "%", "FRED", Category.FINANCIAL_MARKET),

    US_YIELD_SPREAD("US_YIELD_SPREAD", "T10Y2Y", "D",
            "미국 장단기 금리차 (10Y-2Y)", "%p", "FRED", Category.FINANCIAL_MARKET),

    US_VIX("US_VIX", "VIXCLS", "D",
            "VIX 공포지수", "Index", "FRED", Category.FINANCIAL_MARKET),

    US_CREDIT_SPREAD("US_CREDIT_SPREAD", "BAMLH0A0HYM2", "D",
            "하이일드 신용스프레드", "%", "FRED", Category.FINANCIAL_MARKET),

    US_M2("US_M2", "M2SL", "M",
            "미국 M2 통화량", "십억 달러", "FRED", Category.LIQUIDITY),

    US_RRP("US_RRP", "RRPONTSYD", "D",
            "미국 역레포 잔고", "십억 달러", "FRED", Category.LIQUIDITY),

    BRENT_OIL("BRENT_OIL", "DCOILBRENTEU", "D",
            "브렌트유", "달러/배럴", "FRED", Category.COMMODITY),

    COPPER("COPPER", "PCOPPUSDM", "M",
            "구리", "달러/파운드", "FRED", Category.COMMODITY),

    NATURAL_GAS("NATURAL_GAS", "MHHNGSP", "M",
            "천연가스 (Henry Hub)", "달러/MMBtu", "FRED", Category.COMMODITY),

    JPY_USD("JPY_USD", "DEXJPUS", "D",
            "엔/달러 환율", "JPY/USD", "FRED", Category.FOREX),

    CNY_USD("CNY_USD", "DEXCHUS", "D",
            "위안/달러 환율", "CNY/USD", "FRED", Category.FOREX),

    EUR_USD("EUR_USD", "DEXUSEU", "D",
            "유로/달러 환율", "USD/EUR", "FRED", Category.FOREX);

    private final String code;
    private final String seriesId;
    private final String frequency;
    private final String displayName;
    private final String unit;
    private final String source;
    private final Category category;

    FredIndicatorDef(String code, String seriesId, String frequency,
                     String displayName, String unit, String source, Category category) {
        this.code = code;
        this.seriesId = seriesId;
        this.frequency = frequency;
        this.displayName = displayName;
        this.unit = unit;
        this.source = source;
        this.category = category;
    }

    public String code() { return code; }
    public String seriesId() { return seriesId; }
    public String frequency() { return frequency; }
    public String displayName() { return displayName; }
    public String unit() { return unit; }
    public String source() { return source; }
    public Category category() { return category; }

    public static java.util.List<String> codesByCategory(Category category) {
        return java.util.Arrays.stream(values())
                .filter(def -> def.category == category)
                .map(FredIndicatorDef::code)
                .toList();
    }

    public boolean isDaily() { return "D".equals(frequency); }

    public boolean isMonthly() { return "M".equals(frequency); }

    public boolean isQuarterly() { return "Q".equals(frequency); }

    public enum Category {
        POLICY("거시경제 정책"),
        ECONOMY("경제 지표"),
        FINANCIAL_MARKET("금융시장 내부 지표"),
        LIQUIDITY("글로벌 유동성"),
        COMMODITY("원자재"),
        FOREX("환율");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() { return displayName; }
    }
}
