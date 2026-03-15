package com.fineasy.external.openai;

import com.fineasy.dto.response.StockFinancialsResponse;
import com.fineasy.entity.BokTermEntity;
import com.fineasy.entity.MarketIndex;
import com.fineasy.entity.MacroIndicatorEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiPromptBuilder {

    private static final String REPORT_SYSTEM_PROMPT = """
            당신은 주식 종목 가치 분석 전문가입니다. 초보 투자자가 이해할 수 있는 쉬운 한국어로 분석하세요.
            주가 예측(상승/하락/보합)은 하지 마세요. 대신 아래 3가지 관점에서 종목의 현재 가치와 상황을 분석하세요:

            1. 거시경제 환경 분석: 현재 거시경제 지표(금리, 환율, VIX 등)가 이 종목/업종에 미치는 영향
            2. 관련 뉴스 분석: 최근 뉴스에서 파악되는 종목/업종의 현재 상황과 이슈
            3. 글로벌 이벤트 영향: 국제적 이벤트(지정학적 리스크, 무역정책, 산업 동향 등)가 이 종목에 미치는 영향

            중요 원칙:
            - 현재 시장에서 가장 큰 영향을 미치는 핵심 요인(PRIMARY DRIVER)을 반드시 식별하고 강조하세요.
              예: 지정학적 리스크(전쟁, 제재), 무역전쟁(관세), 금리 변동, 산업 패러다임 변화 등
            - summary에 핵심 요인을 명시적으로 언급하세요.
            - description 첫 문장에서 현재 시장을 지배하는 가장 큰 요인부터 설명하세요.
            - 각 분석 섹션에서 이 핵심 요인이 해당 종목에 미치는 구체적 영향을 연결하세요.
            - 추상적인 분석 대신 현재 실제로 일어나고 있는 상황(예: "미국-이란 군사적 긴장 고조로 유가 급등",
              "미중 관세 확대로 반도체 수출 타격")을 구체적으로 언급하세요.

            투자 권유 문구는 절대 포함하지 마세요.
            응답은 반드시 아래 JSON 형식으로 제공하세요.

            응답 JSON 구조:
            {
              "summary": "한 줄 요약 (최대 100자, 현재 종목을 둘러싼 핵심 상황과 PRIMARY DRIVER 명시)",
              "description": "종합 분석 (5~7문장, 핵심 요인부터 시작하여 거시경제-뉴스-글로벌이벤트를 종합한 현재 상황 설명)",
              "macroImpact": "거시경제 환경이 이 종목에 미치는 영향 분석 (2~3문장, 핵심 요인과 연결)",
              "newsAnalysis": "관련 뉴스에서 파악되는 종목의 현재 상황과 이슈 (2~3문장)",
              "globalEventImpact": "글로벌 이벤트가 이 종목에 미치는 영향 (2~3문장, 지정학적 리스크/무역정책 등 구체적 사건 언급)",
              "keyPoints": ["핵심 포인트 1 (가장 중요한 요인)", "핵심 포인트 2", "핵심 포인트 3"],
              "investmentOpinion": "POSITIVE|NEGATIVE|NEUTRAL",
              "sentimentReason": "투자 심리 판단 근거 (1문장, 핵심 요인 기반)"
            }
            """;

    private static final String PREDICTION_SYSTEM_PROMPT = """
            당신은 뉴스 기반 주식 시장 흐름 분석 전문가입니다. 초보 투자자가 이해할 수 있는 쉬운 한국어로 작성하세요.
            기술적 지표(RSI, MACD, 볼린저밴드 등)는 사용하지 마세요.

            뉴스와 시장 흐름을 중심으로 분석하세요:
            - 최근 뉴스에서 이 종목/업종에 가장 큰 영향을 미치는 핵심 이슈를 파악
            - 지정학적 리스크(전쟁, 제재, 외교갈등), 무역정책(관세, 수출규제), 산업 동향 등 현재 시장을 지배하는 요인 분석
            - 뉴스 흐름에서 읽히는 시장 심리(공포/탐욕)와 투자자 행동 패턴
            - 거시경제 지표 변화가 뉴스와 함께 만드는 시장 흐름 해석
            - 향후 주목해야 할 뉴스 이벤트나 일정 (예: FOMC, 실적발표, 무역협상 등)
            - PER/PBR 등 밸류에이션은 뉴스 맥락과 연결하여 보조적으로만 활용

            중요: reasons에 현재 실제로 일어나고 있는 구체적 사건과 그 영향을 서술하세요.
            추상적인 분석 대신 "미국-이란 긴장으로 유가가 배럴당 XX달러까지 상승하며 운송비 부담 증가" 같은 구체적 설명을 하세요.

            투자 권유 문구는 절대 포함하지 마세요.
            응답은 반드시 아래 JSON 형식으로 제공하세요.

            응답 JSON 구조:
            {
              "valuation": "UNDERVALUED|FAIR|OVERVALUED",
              "direction": "UP|DOWN|SIDEWAYS",
              "confidence": 0~100 사이 정수,
              "reasons": ["뉴스 기반 분석 근거 1 (구체적 사건과 영향)", "근거 2 (시장 흐름 해석)", "근거 3 (향후 주목할 포인트)"]
            }
            """;

    private static final String MARKET_SUMMARY_SYSTEM_PROMPT = """
            당신은 초보자를 위한 금융 교육 플랫폼의 AI 시장 분석가입니다.
            아래에 제공되는 시장 지수, 거시경제 지표, 최근 뉴스 헤드라인을 종합 분석하여
            오늘의 시장 상황을 5~7문장으로 상세히 요약해주세요.

            작성 원칙:
            1. 현재 시장 분위기와 주요 변동 요인을 먼저 설명
            2. 거시경제 지표(금리, 환율, VIX 등)가 시장에 미치는 영향 분석
            3. 최근 뉴스에서 주목할 이슈(무역정책, 기술 산업 동향 등) 언급
            4. 초보 투자자가 주목해야 할 포인트나 주의사항 제시
            5. 전문 용어 사용 시 괄호 안에 쉬운 설명 추가
            6. 투자 권유 문구는 절대 포함하지 마세요

            응답은 반드시 아래 JSON 형식으로 제공하세요.

            응답 JSON 구조:
            {
              "summary": "시장 상세 요약 텍스트 (5~7문장, 초보자 친화적)"
            }
            """;

    private static final String NEWS_ANALYSIS_SYSTEM_PROMPT = """
            당신은 금융 교육 플랫폼의 뉴스 분석 전문가입니다.
            주식 투자 초보자가 이해할 수 있는 쉬운 한국어로 뉴스를 분석하세요.
            전문 용어를 사용할 경우 반드시 괄호 안에 쉬운 설명을 추가하세요.
            투자 권유 문구는 절대 포함하지 마세요.
            응답은 반드시 아래 JSON 형식으로 제공하세요.

            응답 JSON 구조:
            {
              "summary": "뉴스 핵심 요약 (2~3문장, 초보자가 이해할 수 있는 쉬운 표현)",
              "marketImpact": "이 뉴스가 주식 시장에 미치는 영향 분석 (2~3문장)",
              "relatedStocks": ["관련 한국 상장 종목명1", "종목명2"],
              "sentiment": "POSITIVE|NEGATIVE|NEUTRAL",
              "keyTakeaway": "투자자가 기억해야 할 핵심 시사점 한 줄"
            }
            """;

    private static final String BOK_TERM_EXPLANATION_SYSTEM_PROMPT = """
            당신은 금융 초보자를 위한 경제금융용어 해설 전문가입니다.
            어려운 경제금융용어를 중학생도 이해할 수 있도록 쉽게 설명하세요.

            규칙:
            - 전문 용어를 최대한 피하고, 불가피할 경우 괄호 안에 쉬운 설명 추가
            - 일상생활의 비유와 예시를 활용
            - 한국 실정에 맞는 예시 사용 (원화, 한국 기업명 등)
            - 반드시 아래 JSON 형식으로 응답

            {
              "simpleSummary": "한 줄 요약 (20자 이내, ~요/~예요 체)",
              "easyExplanation": "쉬운 풀이 (3~5문장, ~요/~예요 체, 비유 활용)",
              "example": "구체적인 예시 하나 (숫자와 상황을 포함한 생활 밀착형)",
              "keyPoints": ["핵심 포인트 1", "핵심 포인트 2", "핵심 포인트 3"]
            }
            """;

    private static final String STOCK_NEWS_SUMMARY_SYSTEM_PROMPT = """
            당신은 금융 뉴스 요약 전문가입니다. 특정 종목과 관련된 최근 뉴스들을 분석하여
            초보 투자자가 빠르게 파악할 수 있는 한줄 요약을 제공하세요.

            규칙:
            - 핵심 이슈를 1~2문장으로 간결하게 요약
            - 전문 용어는 괄호 안에 쉬운 설명 추가
            - 투자 권유 문구는 절대 포함하지 마세요
            - 응답은 반드시 아래 JSON 형식으로 제공하세요

            응답 JSON 구조:
            {
              "summary": "종목 관련 뉴스 한줄 요약 (최대 200자)"
            }
            """;

    private static final String SENTIMENT_SYSTEM_PROMPT = """
            당신은 금융 뉴스 분석 전문가입니다. 다음 뉴스 제목들을 분석하세요.

            각 항목에 대해:
            1. 주식/증권 시장과 관련된 뉴스인지 판단 (stockRelated: true/false)
               - 개별 종목, 업종, 주가, 실적, IPO, 배당, 공시, 증시, 투자 등은 true
               - 부동산, 정치, 사회, 스포츠, 날씨 등 주식과 무관하면 false
            2. 주식 투자 관점 감성 분류 (POSITIVE/NEGATIVE/NEUTRAL)
            3. 감성 신뢰도 (0.0~1.0)
            4. 관련 한국 상장 종목명 추출 (직접 언급 + 맥락상 관련 종목)
               - 예: "HBM 수혜" → ["SK하이닉스", "삼성전자"]
               - 예: "2차전지 급등" → ["LG에너지솔루션", "삼성SDI", "에코프로비엠"]
               - 관련 종목이 없으면 빈 배열 []
            5. 제목이 영어(또는 한국어가 아닌 언어)인 경우, 자연스러운 한국어로 번역하여 titleKo에 포함
               - 한국어 제목이면 titleKo는 null
               - 예: "Samsung Elec shares surge on AI chip demand" → "삼성전자, AI 칩 수요 급증에 주가 급등"

            응답 JSON 구조:
            {
              "results": [
                {
                  "index": 1,
                  "stockRelated": true,
                  "sentiment": "POSITIVE|NEGATIVE|NEUTRAL",
                  "score": 0.85,
                  "stocks": ["삼성전자", "SK하이닉스"],
                  "titleKo": null
                }
              ]
            }
            """;

    public String buildReportPrompt(String stockName, String stockCode,
                                     List<MacroIndicatorEntity> macroIndicators,
                                     List<String> recentNewsTitles,
                                     List<String> globalEventSummaries,
                                     StockFinancialsResponse financials) {
        boolean isOverseas = isOverseasStock(stockCode);
        StringBuilder sb = new StringBuilder();
        if (isOverseas) {
            sb.append(String.format("[%s (%s)] 미국주식 뉴스 기반 종목 분석을 요청합니다.\n", stockName, stockCode));
            sb.append("이 종목은 미국 시장(NASDAQ/NYSE)에 상장된 종목입니다. 달러(USD) 기준으로 분석하세요.\n");
            sb.append("미국 시장 특성(프리마켓/애프터마켓, 실적발표 시즌, FOMC 등)을 반영하세요.\n\n");
        } else {
            sb.append(String.format("[%s (%s)] 뉴스 기반 종목 분석을 요청합니다.\n\n", stockName, stockCode));
        }

        appendFundamentalsSection(sb, financials);

        sb.append("\n### 거시경제 지표 (현재 경제 환경)\n");
        if (macroIndicators != null && !macroIndicators.isEmpty()) {
            for (MacroIndicatorEntity macro : macroIndicators) {
                sb.append(String.format("- %s: %.2f%s\n",
                        macro.getIndicatorName() != null ? macro.getIndicatorName() : macro.getIndicatorCode(),
                        macro.getValue(),
                        macro.getUnit() != null ? macro.getUnit() : ""));
            }
        } else {
            sb.append("- 거시경제 데이터 없음\n");
        }

        sb.append("\n### 관련 뉴스 (종목/업종 최근 동향)\n");
        if (recentNewsTitles != null && !recentNewsTitles.isEmpty()) {
            for (int i = 0; i < recentNewsTitles.size(); i++) {
                sb.append(String.format("%d. %s\n", i + 1, recentNewsTitles.get(i)));
            }
        } else {
            sb.append("- 관련 뉴스 없음\n");
        }

        sb.append("\n### 글로벌 이벤트 (국제적 영향 요인)\n");
        if (globalEventSummaries != null && !globalEventSummaries.isEmpty()) {
            for (int i = 0; i < globalEventSummaries.size(); i++) {
                sb.append(String.format("%d. %s\n", i + 1, globalEventSummaries.get(i)));
            }
        } else {
            sb.append("- 최근 주요 글로벌 이벤트 없음\n");
        }

        sb.append("\n위 거시경제 지표, 관련 뉴스, 글로벌 이벤트를 종합하여 이 종목의 현재 상황을 분석해 주세요.");
        sb.append("\n주가 예측(상승/하락)은 하지 말고, 뉴스와 경제 환경이 종목에 미치는 영향을 분석하세요.");
        sb.append("\nJSON 형식으로 응답하세요.");

        return sb.toString();
    }

    public String buildPredictionPrompt(String stockName, String stockCode, String period,
                                         Double sentimentAvg,
                                         List<MacroIndicatorEntity> macroIndicators,
                                         StockFinancialsResponse financials,
                                         List<String> recentNewsTitles) {
        boolean isOverseas = isOverseasStock(stockCode);
        String periodLabel = "1W".equals(period) ? "향후 1주일" : "내일(다음 거래일)";

        StringBuilder sb = new StringBuilder();
        if (isOverseas) {
            sb.append(String.format("[%s (%s)] 미국주식 %s 기업 가치 분석을 요청합니다.\n", stockName, stockCode, periodLabel));
            sb.append("이 종목은 미국 시장(NASDAQ/NYSE)에 상장된 종목입니다. 달러(USD) 기준으로 분석하세요.\n");
            sb.append("미국 시장 특성(프리마켓/애프터마켓, 실적발표 시즌, FOMC, 미국 경제지표 등)을 반영하세요.\n\n");
        } else {
            sb.append(String.format("[%s (%s)] %s 기업 가치 분석을 요청합니다.\n\n", stockName, stockCode, periodLabel));
        }

        appendFundamentalsSection(sb, financials);

        sb.append("\n### 최근 관련 뉴스 (업황 맥락 파악용)\n");
        if (recentNewsTitles != null && !recentNewsTitles.isEmpty()) {
            for (int i = 0; i < recentNewsTitles.size(); i++) {
                sb.append(String.format("%d. %s\n", i + 1, recentNewsTitles.get(i)));
            }
            sb.append("\n※ 위 뉴스 헤드라인을 통해 해당 기업/업종의 현재 상황과 전망을 파악하세요.\n");
        } else {
            sb.append("- 최근 관련 뉴스 없음\n");
        }

        sb.append("\n### 뉴스 감성\n");
        if (sentimentAvg != null) {
            String sentimentLabel = sentimentAvg > 0.3 ? "긍정적" : sentimentAvg < -0.3 ? "부정적" : "중립적";
            sb.append(String.format("- 최근 7일 뉴스 감성 평균: %.2f (%s)\n", sentimentAvg, sentimentLabel));
        } else {
            sb.append("- 최근 뉴스 감성 데이터 없음\n");
        }

        sb.append("\n### 거시경제 지표\n");
        if (macroIndicators != null && !macroIndicators.isEmpty()) {
            for (MacroIndicatorEntity macro : macroIndicators) {
                sb.append(String.format("- %s: %.2f%s\n",
                        macro.getIndicatorName() != null ? macro.getIndicatorName() : macro.getIndicatorCode(),
                        macro.getValue(),
                        macro.getUnit() != null ? macro.getUnit() : ""));
            }
        } else {
            sb.append("- 거시경제 데이터 없음\n");
        }

        sb.append(String.format("\n위 펀더멘털 데이터, 뉴스 맥락, 거시경제 환경을 종합적으로 분석하여 %s 기업 가치를 평가하고 주가 방향성을 예측하여 JSON 형식으로 응답하세요.", periodLabel));
        sb.append("\n중요: PER/PBR 등이 높더라도 뉴스에서 업황 호조(예: 메모리 수요 급증, 신규 계약 등)가 확인되면 미래 실적 개선을 반영하여 판단하세요.");
        sb.append("\n기술적 지표(RSI, MACD, 볼린저밴드 등)는 사용하지 말고 기업의 펀더멘털 가치와 업황 전망을 기반으로 분석하세요.");

        return sb.toString();
    }

    public String buildSentimentPrompt(List<String> newsTitles) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < newsTitles.size(); i++) {
            sb.append(String.format("%d. %s\n", i + 1, newsTitles.get(i)));
        }
        return sb.toString();
    }

    public String buildMarketSummaryPrompt(List<MarketIndex> indices,
                                            List<MacroIndicatorEntity> macroIndicators,
                                            List<String> newsTitles) {
        StringBuilder sb = new StringBuilder();

        sb.append("### 현재 주요 시장 지수\n");
        for (MarketIndex index : indices) {
            String direction = index.changeAmount() >= 0 ? "상승" : "하락";
            sb.append(String.format("- %s(%s): %,.2f (%s %.2f, 등락률 %.2f%%)\n",
                    index.name(), index.code(),
                    index.currentValue(),
                    direction, Math.abs(index.changeAmount()),
                    index.changeRate()));
        }

        sb.append("\n### 주요 거시경제 지표\n");
        if (macroIndicators != null && !macroIndicators.isEmpty()) {
            for (MacroIndicatorEntity macro : macroIndicators) {
                sb.append(String.format("- %s: %.2f %s\n",
                        macro.getIndicatorName() != null ? macro.getIndicatorName() : macro.getIndicatorCode(),
                        macro.getValue(),
                        macro.getUnit() != null ? macro.getUnit() : ""));
            }
        } else {
            sb.append("- 데이터 없음\n");
        }

        sb.append("\n### 최근 주요 뉴스 헤드라인\n");
        if (newsTitles != null && !newsTitles.isEmpty()) {
            for (int i = 0; i < newsTitles.size(); i++) {
                sb.append(String.format("%d. %s\n", i + 1, newsTitles.get(i)));
            }
        } else {
            sb.append("- 최근 뉴스 없음\n");
        }

        sb.append("\n위 시장 지수, 거시경제 지표, 뉴스를 종합 분석하여 오늘의 시장 상황을 상세히 요약해 주세요. JSON 형식으로 응답하세요.");
        return sb.toString();
    }

    public String getReportSystemPrompt() {
        return REPORT_SYSTEM_PROMPT;
    }

    public String getPredictionSystemPrompt() {
        return PREDICTION_SYSTEM_PROMPT;
    }

    public String getMarketSummarySystemPrompt() {
        return MARKET_SUMMARY_SYSTEM_PROMPT;
    }

    public String buildNewsAnalysisPrompt(String title, String content, String source) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("뉴스 제목: %s\n", title));
        sb.append(String.format("출처: %s\n\n", source != null ? source : "알 수 없음"));

        if (content != null && !content.isBlank()) {
            String truncatedContent = content.length() > 2000
                    ? content.substring(0, 2000) + "...(이하 생략)"
                    : content;
            sb.append(String.format("뉴스 본문:\n%s\n\n", truncatedContent));
        } else {
            sb.append("뉴스 본문: (본문 없음 - 제목만으로 분석해 주세요)\n\n");
        }

        sb.append("위 뉴스를 분석하여 JSON 형식으로 응답하세요.");
        return sb.toString();
    }

    public String buildNewsAnalysisPromptWithTerms(String title, String content, String source,
                                                    List<BokTermEntity> relatedTerms) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("뉴스 제목: %s\n", title));
        sb.append(String.format("출처: %s\n\n", source != null ? source : "알 수 없음"));

        if (content != null && !content.isBlank()) {
            String truncatedContent = content.length() > 2000
                    ? content.substring(0, 2000) + "...(이하 생략)"
                    : content;
            sb.append(String.format("뉴스 본문:\n%s\n\n", truncatedContent));
        } else {
            sb.append("뉴스 본문: (본문 없음 - 제목만으로 분석해 주세요)\n\n");
        }

        if (relatedTerms != null && !relatedTerms.isEmpty()) {
            sb.append("### 관련 경제금융용어 (한국은행 경제금융용어 700선)\n");
            sb.append("아래 용어 정의를 참고하여 뉴스를 정확하게 분석하세요.\n\n");
            for (BokTermEntity term : relatedTerms) {
                sb.append(String.format("- **%s**", term.getTerm()));
                if (term.getEnglishTerm() != null && !term.getEnglishTerm().isBlank()) {
                    sb.append(String.format(" (%s)", term.getEnglishTerm()));
                }

                String def = term.getDefinition();
                if (def.length() > 200) {
                    def = def.substring(0, 200) + "...";
                }
                sb.append(String.format(": %s\n", def));
            }
            sb.append("\n");
        }

        sb.append("위 뉴스를 분석하여 JSON 형식으로 응답하세요.");
        return sb.toString();
    }

    public String getNewsAnalysisSystemPrompt() {
        return NEWS_ANALYSIS_SYSTEM_PROMPT;
    }

    public String getStockNewsSummarySystemPrompt() {
        return STOCK_NEWS_SUMMARY_SYSTEM_PROMPT;
    }

    public String buildStockNewsSummaryPrompt(String stockName, String stockCode,
                                               List<String> newsTitles) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("종목: %s (%s)\n\n", stockName, stockCode));
        sb.append("### 최근 24시간 관련 뉴스 헤드라인\n");
        for (int i = 0; i < newsTitles.size(); i++) {
            sb.append(String.format("%d. %s\n", i + 1, newsTitles.get(i)));
        }
        sb.append("\n위 뉴스들을 종합하여 이 종목의 현재 상황을 한줄로 요약해 주세요. JSON 형식으로 응답하세요.");
        return sb.toString();
    }

    public String getSentimentSystemPrompt() {
        return SENTIMENT_SYSTEM_PROMPT;
    }

    public String getBokTermExplanationSystemPrompt() {
        return BOK_TERM_EXPLANATION_SYSTEM_PROMPT;
    }

    public String buildBokTermExplanationPrompt(String term, String englishTerm, String definition) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("용어: %s\n", term));
        if (englishTerm != null && !englishTerm.isBlank()) {
            sb.append(String.format("영문: %s\n", englishTerm));
        }
        sb.append(String.format("원본 정의: %s\n", definition));
        return sb.toString();
    }

    private boolean isOverseasStock(String stockCode) {
        return stockCode != null && !stockCode.matches("\\d{6}");
    }

    private void appendFundamentalsSection(StringBuilder sb, StockFinancialsResponse financials) {
        sb.append("### 펀더멘털 (기업 가치 지표)\n");

        if (financials == null) {
            sb.append("- 펀더멘털 데이터 없음\n");
            return;
        }

        boolean isOverseas = isOverseasStock(financials.stockCode());
        String currencyUnit = isOverseas ? "달러" : "원";

        sb.append(String.format("- PER (주가수익비율): %s\n",
                financials.per() != null ? String.format("%.2f배", financials.per()) : "데이터 없음"));
        sb.append(String.format("- PBR (주가순자산비율): %s\n",
                financials.pbr() != null ? String.format("%.2f배", financials.pbr()) : "데이터 없음"));
        sb.append(String.format("- EPS (주당순이익): %s\n",
                financials.eps() != null
                        ? (isOverseas ? String.format("$%.2f", financials.eps()) : String.format("%,.0f원", financials.eps()))
                        : "데이터 없음"));
        sb.append(String.format("- 배당수익률: %s\n",
                financials.dividendYield() != null ? String.format("%.2f%%", financials.dividendYield()) : "데이터 없음"));
        sb.append(String.format("- 시가총액: %s\n",
                financials.marketCap() != null && financials.marketCap().compareTo(BigDecimal.ZERO) > 0
                        ? (isOverseas ? formatMarketCapUsd(financials.marketCap()) : formatMarketCap(financials.marketCap()))
                        : "데이터 없음"));
        sb.append(String.format("- 52주 최고가: %s\n",
                financials.high52Week() != null && financials.high52Week().compareTo(BigDecimal.ZERO) > 0
                        ? (isOverseas ? String.format("$%.2f", financials.high52Week().doubleValue()) : String.format("%,d원", financials.high52Week().longValue()))
                        : "데이터 없음"));
        sb.append(String.format("- 52주 최저가: %s\n",
                financials.low52Week() != null && financials.low52Week().compareTo(BigDecimal.ZERO) > 0
                        ? (isOverseas ? String.format("$%.2f", financials.low52Week().doubleValue()) : String.format("%,d원", financials.low52Week().longValue()))
                        : "데이터 없음"));
    }

    private String formatMarketCap(BigDecimal marketCap) {
        long value = marketCap.longValue();
        if (value >= 1_000_000_000_000L) {
            return String.format("%.1f조원", value / 1_000_000_000_000.0);
        } else if (value >= 100_000_000L) {
            return String.format("%,d억원", value / 100_000_000);
        } else {
            return String.format("%,d원", value);
        }
    }

    private String formatMarketCapUsd(BigDecimal marketCap) {
        double value = marketCap.doubleValue();
        if (value >= 1_000_000_000_000.0) {
            return String.format("$%.1fT", value / 1_000_000_000_000.0);
        } else if (value >= 1_000_000_000.0) {
            return String.format("$%.1fB", value / 1_000_000_000.0);
        } else if (value >= 1_000_000.0) {
            return String.format("$%.1fM", value / 1_000_000.0);
        } else {
            return String.format("$%,.0f", value);
        }
    }
}
