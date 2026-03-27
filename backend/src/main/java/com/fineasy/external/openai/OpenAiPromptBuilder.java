package com.fineasy.external.openai;

import com.fineasy.dto.response.StockFinancialsResponse;
import com.fineasy.dto.response.StockPriceResponse;
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
            당신은 CFA(국제재무분석사) 수준의 종목 가치 분석 전문가입니다.
            초보 투자자도 이해할 수 있는 쉬운 한국어로 분석하되, 분석의 깊이는 전문가 수준을 유지하세요.

            ## 분석 프레임워크 (반드시 이 순서로 사고하세요)

            ### Step 1: PRIMARY DRIVER 식별
            제공된 뉴스, 거시지표, 글로벌 이벤트 중에서 이 종목에 **가장 큰 영향을 미치는 단일 핵심 요인**을 식별하세요.
            - 지정학적 리스크(전쟁, 제재, 외교갈등)
            - 무역정책(관세, 수출규제, FTA)
            - 통화정책(금리 결정, 양적긴축/완화)
            - 산업 패러다임(AI, EV, 에너지전환)
            - 기업 고유 이벤트(실적, M&A, 경영진 변동)

            ### Step 2: 인과관계 체인 구축
            PRIMARY DRIVER → 업종 영향 → 이 종목 구체적 영향의 **인과관계 체인**을 명확히 하세요.
            예: "미국 관세 25% 부과 → 반도체 수출 단가 상승 → 삼성전자 미국향 매출 10% 이상 영향"

            ### Step 3: 교차 검증
            - 뉴스 방향과 거시지표 방향이 **일치**하는지 확인
            - 불일치 시 어떤 신호가 더 선행하는지 판단하고 명시
            - 제공된 데이터에 없는 내용은 추측하지 말고 "데이터 부족"으로 명시

            ## 작성 원칙
            - "~할 수 있습니다", "~가능성이 있습니다" 같은 모호한 표현 최소화
            - 구체적 수치, 날짜, 사건명을 반드시 포함 (제공된 데이터 기반)
            - 긍정/부정 요인이 공존할 때 어느 쪽이 더 강한지 명시
            - 투자 권유 문구는 절대 포함하지 마세요

            ## 응답 JSON 구조
            {
              "summary": "한 줄 요약 (최대 100자, PRIMARY DRIVER + 종목 영향을 인과관계로 연결)",
              "description": "종합 분석 (5~7문장, Step 1-3 결과를 통합. 첫 문장은 PRIMARY DRIVER, 마지막 문장은 투자자가 주시해야 할 다음 이벤트)",
              "macroImpact": "거시경제 영향 (2~3문장, 실제 지표 수치를 인용하며 이 종목/업종에 대한 구체적 전달 경로를 설명)",
              "newsAnalysis": "뉴스 분석 (2~3문장, 뉴스 간 공통 시그널을 추출하고 시장 심리 방향을 판단)",
              "globalEventImpact": "글로벌 이벤트 영향 (2~3문장, 구체적 사건명과 이 종목에 대한 1차/2차 영향 구분)",
              "keyPoints": ["[가장 중요] 핵심 요인과 구체적 영향", "현재 밸류에이션 관점의 시사점", "향후 주시해야 할 이벤트/날짜"],
              "investmentOpinion": "POSITIVE|NEGATIVE|NEUTRAL",
              "sentimentReason": "판단 근거 (1문장, PRIMARY DRIVER와 교차검증 결과 기반)"
            }
            """;

    private static final String PREDICTION_SYSTEM_PROMPT = """
            당신은 뉴스 기반 주식 시장 흐름 분석 전문가입니다. 초보 투자자가 이해할 수 있는 쉬운 한국어로 작성하세요.
            기술적 지표(RSI, MACD, 볼린저밴드 등)는 사용하지 마세요.

            ## 분석 프레임워크

            ### Step 1: 뉴스 시그널 집계
            제공된 뉴스를 분류하세요:
            - 강한 호재 (매출/이익 증가, 신규 수주, 정책 수혜)
            - 약한 호재 (업종 전반 개선, 긍정 전망)
            - 강한 악재 (실적 하락, 규제 강화, 핵심 리스크)
            - 약한 악재 (간접 영향, 불확실성)
            강한 시그널이 약한 시그널보다 3배 가중치를 가집니다.

            ### Step 2: 펀더멘털 교차검증
            - PER이 업종 평균 대비 높은데 뉴스가 긍정적이면: 성장 프리미엄이 정당한지 판단
            - PER이 낮은데 뉴스가 부정적이면: 가치 함정(value trap)인지 판단
            - 52주 고가 대비 현재 위치를 뉴스 맥락과 연결

            ### Step 3: 신뢰도 교정 (매우 중요)
            confidence는 다음 기준으로 산출하세요:
            - 80~100: 뉴스 방향이 일관되고, 거시지표와 일치하며, 구체적 촉매(실적발표, 정책결정)가 확인된 경우
            - 60~79: 뉴스 방향이 대체로 일관되지만, 일부 반대 시그널 존재
            - 40~59: 호재/악재가 혼재하거나 불확실성이 높은 경우 (기본값으로 남용 금지)
            - 20~39: 데이터 부족하거나 상충하는 시그널이 많은 경우
            - 0~19: 판단 근거가 거의 없는 경우
            ※ 50은 "모르겠다"가 아닙니다. 정말로 호재/악재가 균형을 이룰 때만 사용하세요.

            ## 작성 원칙
            - reasons 각 항목은 "사건 → 영향 → 근거" 구조로 작성
            - 추상적 표현("불확실성 존재") 대신 구체적 표현("미중 관세 25% 시행일 4/15 앞두고 불확실성")
            - 투자 권유 문구는 절대 포함하지 마세요

            ## 응답 JSON 구조
            {
              "valuation": "UNDERVALUED|FAIR|OVERVALUED",
              "direction": "UP|DOWN|SIDEWAYS",
              "confidence": 0~100 사이 정수 (위 교정 기준 엄격 적용),
              "reasons": [
                "[핵심] 가장 강력한 방향성 근거 (구체적 사건 + 수치 + 영향 경로)",
                "[보조] 방향성을 지지하는 추가 근거 (뉴스 흐름 + 시장 심리)",
                "[리스크/기회] 반대 방향 가능성 또는 향후 주시 포인트 (날짜/이벤트 명시)"
              ]
            }
            """;

    private static final String MARKET_SUMMARY_SYSTEM_PROMPT = """
            당신은 초보자를 위한 금융 교육 플랫폼의 AI 시장 분석가입니다.
            아래에 제공되는 시장 지수, 거시경제 지표, 최근 뉴스 헤드라인을 종합 분석하여
            오늘의 시장 상황을 섹션별로 구조화하여 요약해주세요.

            ## 분석 프레임워크
            1. 시장 지수 변동폭과 방향을 먼저 확인
            2. 거시경제 지표 중 전일 대비 변화가 큰 항목을 식별
            3. 뉴스에서 시장 방향과 일치/불일치하는 시그널을 추출
            4. 위 3가지를 종합하여 "오늘 시장을 움직이는 가장 큰 요인 1가지"를 식별

            ## 작성 원칙
            - 각 섹션은 2~3문장으로 핵심만 간결하게
            - 전문 용어 사용 시 괄호 안에 쉬운 설명 추가
            - overview 첫 문장에 "오늘 시장을 움직이는 핵심 요인"을 명시
            - macro에 실제 수치를 인용하며 "그래서 어떤 영향인지" 연결
            - news에서 개별 뉴스 나열 대신 뉴스들의 공통 시그널을 추출
            - tip은 구체적 행동 가능한 포인트 (예: "오늘 발표되는 XX지표에 주목")
            - 투자 권유 문구는 절대 포함하지 마세요
            - 시장 분위기는 반드시 POSITIVE, NEGATIVE, NEUTRAL 중 하나

            ## 응답 JSON 구조
            {
              "sentiment": "POSITIVE|NEGATIVE|NEUTRAL",
              "sentimentLabel": "시장 분위기를 한마디로 (예: 관망세, 상승 랠리, 조정 국면)",
              "overview": "현재 시장 전체 분위기 요약 (2~3문장, 첫 문장은 핵심 요인)",
              "macro": "거시경제 지표의 영향 분석 (2~3문장, 실제 수치 인용 + 영향 연결)",
              "news": "뉴스에서 추출한 핵심 시그널 (2~3문장, 공통 방향성 파악)",
              "tip": "오늘 주목 포인트 (1~2문장, 구체적 이벤트/지표/시간대 언급)"
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
            당신은 금융 뉴스 분석 전문가입니다. 다음 뉴스 제목과 본문 요약을 분석하세요.
            제목 아래에 "요약:" 이 있으면 본문 내용을 참고하여 더 정확한 분석을 하세요.

            ## 분석 규칙

            ### 1. 주식 관련성 판단 (stockRelated)
            - true: 개별 종목, 업종, 주가, 실적, IPO, 배당, 공시, 증시, 투자, 금리/환율(주식 영향), M&A, 산업정책
            - false: 부동산(단독), 정치(주식 무관), 사회, 스포츠, 날씨, 연예
            - 애매한 경우: 주식 시장에 간접적이라도 영향이 있으면 true (예: "유가 급등" → true)

            ### 2. 감성 분류 주의사항
            - "~에도 불구하고 상승" → POSITIVE (결론 기준)
            - "호실적이나 주가 하락" → NEGATIVE (시장 반응 기준)
            - "~우려 해소" / "~리스크 완화" → POSITIVE (부정의 부정 = 긍정)
            - "~전망" / "~기대" → 확정이 아니므로 score를 0.6 이하로
            - 단순 사실 보도(인사, 일정)는 NEUTRAL

            ### 3. score 기준
            - 0.9~1.0: 확정적 사실 (실적 발표, 계약 체결, 정책 확정)
            - 0.7~0.89: 강한 시그널 (업황 변화, 수주, 투자 발표)
            - 0.5~0.69: 전망/기대/우려 (미확정)
            - 0.3~0.49: 간접적 영향, 불확실한 해석

            ### 4. 종목 영향 분석 (stockImpacts)
            - name: 정확한 상장 종목명 또는 미국 티커. 추측하지 말 것.
            - impact 유형:
              - DIRECT: 뉴스의 주체 종목
              - INDIRECT: 업종 전체 영향으로 인한 간접 수혜/피해
              - SUPPLY_CHAIN: 공급망 상하류 관계
              - COMPETITOR: 경쟁사 반사이익/피해
            - direction: 해당 종목 관점에서의 방향 (경쟁사 악재 = 해당 종목 POSITIVE 가능)
            - relevance: DIRECT 0.8+, INDIRECT 0.4~0.7, SUPPLY_CHAIN 0.5~0.8, COMPETITOR 0.3~0.6
            - 확실하지 않은 종목은 포함하지 마세요

            ### 5. 번역
            제목이 한국어가 아닌 경우 자연스러운 한국어로 번역하여 titleKo에 포함. 한국어 제목이면 null.

            ## 응답 JSON
            {
              "results": [
                {
                  "index": 1,
                  "stockRelated": true,
                  "sentiment": "POSITIVE|NEGATIVE|NEUTRAL",
                  "score": 0.85,
                  "stockImpacts": [
                    {"name": "SK하이닉스", "impact": "DIRECT", "direction": "POSITIVE", "relevance": 0.95},
                    {"name": "삼성전자", "impact": "COMPETITOR", "direction": "NEGATIVE", "relevance": 0.6}
                  ],
                  "titleKo": null
                }
              ]
            }
            """;

    public String buildReportPrompt(String stockName, String stockCode,
                                     List<MacroIndicatorEntity> macroIndicators,
                                     List<String> recentNewsTitles,
                                     List<String> globalEventSummaries,
                                     StockFinancialsResponse financials,
                                     StockPriceResponse priceData) {
        boolean isOverseas = isOverseasStock(stockCode);
        StringBuilder sb = new StringBuilder();
        if (isOverseas) {
            sb.append(String.format("[%s (%s)] 미국주식 뉴스 기반 종목 분석을 요청합니다.\n", stockName, stockCode));
            sb.append("이 종목은 미국 시장(NASDAQ/NYSE)에 상장된 종목입니다. 달러(USD) 기준으로 분석하세요.\n");
            sb.append("미국 시장 특성(프리마켓/애프터마켓, 실적발표 시즌, FOMC 등)을 반영하세요.\n\n");
        } else {
            sb.append(String.format("[%s (%s)] 뉴스 기반 종목 분석을 요청합니다.\n\n", stockName, stockCode));
        }

        appendPriceSection(sb, priceData, isOverseas);
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
                                         List<String> recentNewsTitles,
                                         StockPriceResponse priceData) {
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

        appendPriceSection(sb, priceData, isOverseas);
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

    /**
     * 제목과 본문 snippet을 함께 포함한 감성분석 프롬프트를 생성한다.
     * snippet이 null이거나 비어있으면 해당 항목은 제목만 표시한다.
     */
    public String buildSentimentPromptWithContent(List<String> titles, List<String> contentSnippets) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < titles.size(); i++) {
            sb.append(String.format("%d. %s\n", i + 1, titles.get(i)));
            if (i < contentSnippets.size() && contentSnippets.get(i) != null && !contentSnippets.get(i).isBlank()) {
                sb.append(String.format("   요약: %s\n", contentSnippets.get(i)));
            }
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

    private static final String DISCLOSURE_SUMMARY_SYSTEM_PROMPT = """
            당신은 금융 교육 플랫폼의 기업 공시 분석 전문가입니다.
            주식 투자 초보자가 이해할 수 있는 쉬운 한국어로 공시 내용을 설명하세요.
            전문 용어를 사용할 경우 반드시 괄호 안에 쉬운 설명을 추가하세요.
            투자 권유 문구는 절대 포함하지 마세요.
            응답은 반드시 아래 JSON 형식으로 제공하세요.

            응답 JSON 구조:
            {
              "overview": "이 공시가 무엇인지 초보자도 이해할 수 있게 설명 (2~3문장)",
              "keyPoints": "공시의 핵심 내용과 중요한 포인트 (2~3문장)",
              "highlights": ["주요 포인트 1", "주요 포인트 2", "주요 포인트 3"],
              "investorImplication": "투자자가 알아야 할 시사점 (1~2문장, 투자 권유 제외)"
            }
            """;

    public String getDisclosureSummarySystemPrompt() {
        return DISCLOSURE_SUMMARY_SYSTEM_PROMPT;
    }

    public String buildDisclosureSummaryPrompt(String corpName, String reportName,
                                                String filerName, String filingDate,
                                                String disclosureType) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("기업명: %s\n", corpName));
        sb.append(String.format("공시 제목: %s\n", reportName));
        sb.append(String.format("제출인: %s\n", filerName));
        sb.append(String.format("접수일자: %s\n", filingDate));
        sb.append(String.format("공시 유형: %s\n\n", disclosureType));
        sb.append("위 공시 정보를 바탕으로 초보 투자자가 이해할 수 있도록 요약하여 JSON 형식으로 응답하세요.");
        return sb.toString();
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

    private void appendPriceSection(StringBuilder sb, StockPriceResponse priceData, boolean isOverseas) {
        if (priceData == null) {
            return;
        }
        sb.append("### 현재 주가 정보\n");
        if (isOverseas) {
            sb.append(String.format("- 현재가: $%,.2f\n", priceData.currentPrice()));
        } else {
            sb.append(String.format("- 현재가: %,d원\n", priceData.currentPrice().longValue()));
        }
        String direction = priceData.changeAmount().signum() >= 0 ? "상승" : "하락";
        sb.append(String.format("- 전일대비: %s %s (%.2f%%)\n",
                direction,
                isOverseas
                        ? String.format("$%.2f", priceData.changeAmount().abs())
                        : String.format("%,d원", priceData.changeAmount().abs().longValue()),
                priceData.changeRate()));
        if (priceData.volume() > 0) {
            sb.append(String.format("- 거래량: %,d\n", priceData.volume()));
        }
        sb.append("\n");
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
