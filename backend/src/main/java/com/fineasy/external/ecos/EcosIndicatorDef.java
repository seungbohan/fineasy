package com.fineasy.external.ecos;

import com.fineasy.external.fred.FredIndicatorDef;

import java.util.Arrays;
import java.util.List;

public enum EcosIndicatorDef {

    KR_BASE_RATE("KR_BASE_RATE", "722Y001", "0101000", "D",
            "한국은행 기준금리", "%", "ECOS", FredIndicatorDef.Category.POLICY),

    KR_CPI("KR_CPI", "901Y009", "0", "M",
            "소비자물가지수", "Index 2020=100", "ECOS", FredIndicatorDef.Category.ECONOMY),

    KR_GDP_GROWTH("KR_GDP_GROWTH", "200Y002", "10111", "Q",
            "GDP 성장률", "%", "ECOS", FredIndicatorDef.Category.ECONOMY),

    KR_UNEMPLOYMENT("KR_UNEMPLOYMENT", "901Y027", "I61BC", "M",
            "실업률", "%", "ECOS", FredIndicatorDef.Category.ECONOMY),

    KR_INDUSTRIAL_PROD("KR_INDUSTRIAL_PROD", "901Y033", "A00", "M",
            "산업생산지수", "Index 2020=100", "ECOS", FredIndicatorDef.Category.ECONOMY),

    KR_TREASURY_3Y("KR_TREASURY_3Y", "817Y002", "010200000", "D",
            "국고채 3년", "%", "ECOS", FredIndicatorDef.Category.FINANCIAL_MARKET),

    KR_TREASURY_10Y("KR_TREASURY_10Y", "817Y002", "010210000", "D",
            "국고채 10년", "%", "ECOS", FredIndicatorDef.Category.FINANCIAL_MARKET),

    KR_CD_91D("KR_CD_91D", "817Y002", "010400000", "D",
            "CD(91일) 금리", "%", "ECOS", FredIndicatorDef.Category.FINANCIAL_MARKET),

    KR_USD_KRW("KR_USD_KRW", "731Y001", "0000001", "D",
            "원/달러 환율", "원/달러", "ECOS", FredIndicatorDef.Category.FOREX),

    KR_EUR_KRW("KR_EUR_KRW", "731Y001", "0000053", "D",
            "유로/원 환율", "원/유로", "ECOS", FredIndicatorDef.Category.FOREX),

    KR_JPY_KRW("KR_JPY_KRW", "731Y001", "0000002", "D",
            "엔/원 환율 (100엔)", "원/100엔", "ECOS", FredIndicatorDef.Category.FOREX),

    KR_M2("KR_M2", "161Y006", "BBHA00", "M",
            "M2 통화량", "십억원", "ECOS", FredIndicatorDef.Category.LIQUIDITY);

    private final String code;
    private final String statCode;
    private final String itemCode1;
    private final String cycle;
    private final String displayName;
    private final String unit;
    private final String source;
    private final FredIndicatorDef.Category category;

    EcosIndicatorDef(String code, String statCode, String itemCode1, String cycle,
                     String displayName, String unit, String source,
                     FredIndicatorDef.Category category) {
        this.code = code;
        this.statCode = statCode;
        this.itemCode1 = itemCode1;
        this.cycle = cycle;
        this.displayName = displayName;
        this.unit = unit;
        this.source = source;
        this.category = category;
    }

    public String code() { return code; }
    public String statCode() { return statCode; }
    public String itemCode1() { return itemCode1; }
    public String cycle() { return cycle; }
    public String displayName() { return displayName; }
    public String unit() { return unit; }
    public String source() { return source; }
    public FredIndicatorDef.Category category() { return category; }

    public boolean isDaily() { return "D".equals(cycle); }

    public boolean isMonthly() { return "M".equals(cycle); }

    public boolean isQuarterly() { return "Q".equals(cycle); }

    public static List<String> codesByCategory(FredIndicatorDef.Category category) {
        return Arrays.stream(values())
                .filter(def -> def.category == category)
                .map(EcosIndicatorDef::code)
                .toList();
    }
}
