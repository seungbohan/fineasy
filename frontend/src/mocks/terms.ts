import { FinancialTerm } from '@/types';

export const TERM_CATEGORIES = [
  { id: 'stock-basics', name: '주식 기초', displayOrder: 1 },
  { id: 'financial-metrics', name: '재무 지표', displayOrder: 2 },
  { id: 'technical-analysis', name: '기술적 분석', displayOrder: 3 },
  { id: 'bonds-rates', name: '채권/금리', displayOrder: 4 },
  { id: 'macroeconomics', name: '거시경제', displayOrder: 5 },
  { id: 'derivatives', name: '파생상품', displayOrder: 6 },
];

export const MOCK_TERMS: FinancialTerm[] = [

  {
    id: 1,
    name: '주식',
    nameEn: 'Stock',
    category: '주식 기초',
    difficulty: 'BEGINNER',
    simpleDescription:
      '회사의 소유권을 나누어 놓은 것입니다. 주식을 사면 그 회사의 주인 중 한 명이 됩니다.',
    detailedDescription:
      '주식(Stock)은 기업이 자금을 모으기 위해 발행하는 유가증권입니다. 주식을 보유하면 해당 기업의 주주가 되어 의결권과 배당금을 받을 권리를 갖습니다. 주가는 수요와 공급에 의해 결정되며, 기업의 실적, 경제 상황, 시장 심리 등 다양한 요인에 영향을 받습니다.',
    exampleSentence:
      '삼성전자 주식을 10주 샀다면, 삼성전자라는 회사의 아주 작은 부분을 소유하게 된 것입니다.',
    relatedTerms: [
      { id: 2, name: '주가' },
      { id: 3, name: '시가총액' },
      { id: 4, name: '거래량' },
    ],
  },
  {
    id: 2,
    name: '주가',
    nameEn: 'Stock Price',
    category: '주식 기초',
    difficulty: 'BEGINNER',
    simpleDescription:
      '주식 한 장의 가격입니다. 사고 싶은 사람이 많으면 오르고, 팔고 싶은 사람이 많으면 내립니다.',
    detailedDescription:
      '주가는 증권시장에서 거래되는 주식의 가격을 말합니다. 주가는 매수자와 매도자의 수요와 공급에 의해 실시간으로 변동합니다. 기업의 실적, 경제 지표, 업계 동향, 글로벌 이벤트 등이 주가에 영향을 미칩니다.',
    exampleSentence:
      '삼성전자의 현재 주가가 72,000원이라면, 주식 한 장을 사려면 72,000원이 필요합니다.',
    relatedTerms: [
      { id: 1, name: '주식' },
      { id: 5, name: '시가' },
    ],
  },
  {
    id: 3,
    name: '시가총액',
    nameEn: 'Market Capitalization',
    category: '주식 기초',
    difficulty: 'BEGINNER',
    simpleDescription:
      '회사의 전체 가치를 나타내는 숫자입니다. 주가에 발행된 주식 수를 곱한 값입니다.',
    detailedDescription:
      '시가총액(Market Cap)은 상장 기업의 총 시장 가치를 나타내며, 현재 주가 x 발행주식 총수로 계산됩니다. 시가총액이 크면 대형주, 작으면 소형주로 분류됩니다. 코스피 시가총액 1위는 삼성전자입니다.',
    exampleSentence:
      '삼성전자의 시가총액이 440조원이라는 것은, 삼성전자의 모든 주식을 합친 가치가 440조원이라는 뜻입니다.',
    relatedTerms: [
      { id: 1, name: '주식' },
      { id: 2, name: '주가' },
    ],
  },
  {
    id: 4,
    name: '거래량',
    nameEn: 'Trading Volume',
    category: '주식 기초',
    difficulty: 'BEGINNER',
    simpleDescription:
      '하루 동안 사고팔린 주식의 수입니다. 거래량이 많으면 그 주식에 관심이 많다는 뜻입니다.',
    detailedDescription:
      '거래량은 일정 기간 동안 거래된 주식의 총 수량을 의미합니다. 거래량이 급증하면 해당 종목에 대한 시장 참여자들의 관심이 높아졌다는 신호일 수 있습니다. 기술적 분석에서 거래량은 가격 움직임의 신뢰성을 판단하는 중요한 지표입니다.',
    exampleSentence:
      '삼성전자의 오늘 거래량이 1,800만 주라면, 오늘 하루 동안 1,800만 장의 삼성전자 주식이 거래된 것입니다.',
    relatedTerms: [
      { id: 1, name: '주식' },
      { id: 2, name: '주가' },
    ],
  },
  {
    id: 5,
    name: '시가',
    nameEn: 'Opening Price',
    category: '주식 기초',
    difficulty: 'BEGINNER',
    simpleDescription:
      '장이 열렸을 때 처음 거래된 가격입니다. 하루의 시작 가격이라고 생각하면 됩니다.',
    detailedDescription:
      '시가(Opening Price)는 증권시장이 개장한 후 처음으로 체결된 거래 가격입니다. 전일 종가와 비교하여 갭 상승 또는 갭 하락 여부를 판단하는 기준이 됩니다.',
    exampleSentence:
      '삼성전자의 오늘 시가가 73,000원이라면, 오늘 장이 열리자마자 73,000원에 첫 거래가 이루어진 것입니다.',
    relatedTerms: [
      { id: 2, name: '주가' },
      { id: 6, name: '종가' },
    ],
  },
  {
    id: 6,
    name: '종가',
    nameEn: 'Closing Price',
    category: '주식 기초',
    difficulty: 'BEGINNER',
    simpleDescription:
      '장이 마감될 때 마지막으로 거래된 가격입니다. 그날의 최종 가격입니다.',
    detailedDescription:
      '종가(Closing Price)는 증권시장 폐장 시 마지막으로 체결된 거래 가격입니다. 종가는 다음 날 시가와의 비교, 기술적 분석, 펀드 순자산가치(NAV) 계산 등에 기준이 됩니다.',
    exampleSentence:
      '삼성전자의 종가가 74,200원이라면, 오늘 장 마감 시점의 최종 가격이 74,200원인 것입니다.',
    relatedTerms: [
      { id: 5, name: '시가' },
      { id: 2, name: '주가' },
    ],
  },
  {
    id: 7,
    name: '공매도',
    nameEn: 'Short Selling',
    category: '주식 기초',
    difficulty: 'INTERMEDIATE',
    simpleDescription:
      '주식을 빌려서 먼저 팔고, 나중에 더 싼 가격에 사서 갚는 투자 방법입니다.',
    detailedDescription:
      '공매도(Short Selling)는 보유하지 않은 주식을 빌려서 매도한 후, 이후 주가가 하락하면 낮은 가격에 매수하여 차익을 얻는 투자 전략입니다. 주가 하락에 베팅하는 방법으로, 높은 위험을 수반합니다.',
    exampleSentence:
      '주가가 10만원일 때 공매도하고, 8만원으로 떨어졌을 때 사서 갚으면 2만원의 이익을 얻습니다.',
    relatedTerms: [
      { id: 1, name: '주식' },
      { id: 4, name: '거래량' },
    ],
  },

  {
    id: 8,
    name: 'PER',
    nameEn: 'Price-Earnings Ratio',
    category: '재무 지표',
    difficulty: 'BEGINNER',
    simpleDescription:
      '주가가 회사가 버는 돈의 몇 배인지 나타내는 숫자입니다. 낮을수록 저평가, 높을수록 고평가로 봅니다.',
    detailedDescription:
      'PER(주가수익비율, Price-Earnings Ratio)은 현재 주가를 주당순이익(EPS)으로 나눈 값입니다. PER = 주가 / EPS. PER이 낮으면 이익 대비 주가가 저렴하다는 의미이고, 높으면 비싸다는 의미입니다. 같은 업종 내에서 비교하는 것이 의미있습니다.',
    exampleSentence:
      '삼성전자의 PER이 14.8배라는 것은, 현재 주가가 연간 순이익의 14.8배 수준이라는 뜻입니다.',
    relatedTerms: [
      { id: 9, name: 'PBR' },
      { id: 10, name: 'EPS' },
      { id: 11, name: 'ROE' },
    ],
  },
  {
    id: 9,
    name: 'PBR',
    nameEn: 'Price-Book Ratio',
    category: '재무 지표',
    difficulty: 'BEGINNER',
    simpleDescription:
      '주가가 회사의 장부가치(순자산)의 몇 배인지 나타내는 숫자입니다. 1 이하이면 회사의 자산보다 싸게 거래되는 것입니다.',
    detailedDescription:
      'PBR(주가순자산비율, Price-Book Ratio)은 현재 주가를 주당순자산(BPS)으로 나눈 값입니다. PBR = 주가 / BPS. PBR이 1보다 낮으면 회사의 순자산 가치보다 주가가 낮다는 의미로 저평가 상태로 볼 수 있습니다.',
    exampleSentence:
      '현대차의 PBR이 0.76배라면, 주가가 회사 자산 가치의 76% 수준에 거래되고 있어 저평가 가능성이 있습니다.',
    relatedTerms: [
      { id: 8, name: 'PER' },
      { id: 10, name: 'EPS' },
    ],
  },
  {
    id: 10,
    name: 'EPS',
    nameEn: 'Earnings Per Share',
    category: '재무 지표',
    difficulty: 'BEGINNER',
    simpleDescription:
      '회사가 번 돈을 주식 한 장당으로 나눈 값입니다. 높을수록 돈을 잘 버는 회사입니다.',
    detailedDescription:
      'EPS(주당순이익, Earnings Per Share)는 기업의 순이익을 발행주식 수로 나눈 값입니다. EPS = 순이익 / 발행주식수. EPS가 높을수록 기업의 수익성이 좋다는 의미이며, PER 계산의 기초가 됩니다.',
    exampleSentence:
      '삼성전자의 EPS가 5,012원이라면, 주식 한 장당 5,012원의 이익을 내고 있다는 뜻입니다.',
    relatedTerms: [
      { id: 8, name: 'PER' },
      { id: 9, name: 'PBR' },
    ],
  },
  {
    id: 11,
    name: 'ROE',
    nameEn: 'Return on Equity',
    category: '재무 지표',
    difficulty: 'INTERMEDIATE',
    simpleDescription:
      '회사가 주주의 돈을 얼마나 효율적으로 사용해서 이익을 냈는지 보여주는 비율입니다.',
    detailedDescription:
      'ROE(자기자본이익률, Return on Equity)는 순이익을 자기자본으로 나눈 값입니다. ROE = 순이익 / 자기자본 x 100%. ROE가 높으면 주주가 투입한 자본 대비 높은 수익을 올리고 있다는 의미입니다.',
    exampleSentence:
      'ROE가 15%라면, 주주가 투자한 100원으로 15원의 순이익을 벌었다는 뜻입니다.',
    relatedTerms: [
      { id: 8, name: 'PER' },
      { id: 10, name: 'EPS' },
    ],
  },
  {
    id: 12,
    name: '배당수익률',
    nameEn: 'Dividend Yield',
    category: '재무 지표',
    difficulty: 'BEGINNER',
    simpleDescription:
      '주식을 가지고 있으면 받을 수 있는 배당금이 주가의 몇 %인지 나타냅니다.',
    detailedDescription:
      '배당수익률은 주당 배당금을 현재 주가로 나눈 비율입니다. 배당수익률 = (주당 배당금 / 주가) x 100%. 배당수익률이 높은 주식은 안정적인 수익을 원하는 투자자에게 매력적입니다.',
    exampleSentence:
      '기아의 배당수익률이 4.33%라면, 100만원어치 주식을 보유하면 연간 약 43,300원의 배당금을 받을 수 있습니다.',
    relatedTerms: [
      { id: 8, name: 'PER' },
      { id: 1, name: '주식' },
    ],
  },

  {
    id: 13,
    name: '이동평균선',
    nameEn: 'Moving Average',
    category: '기술적 분석',
    difficulty: 'INTERMEDIATE',
    simpleDescription:
      '일정 기간 동안의 주가 평균을 연결한 선입니다. 주가의 큰 흐름을 파악하는 데 사용합니다.',
    detailedDescription:
      '이동평균선(MA)은 일정 기간의 종가 평균을 계산하여 그래프로 표시한 것입니다. 5일, 20일, 60일, 120일 이동평균선이 주로 사용됩니다. 단기 이동평균선이 장기 이동평균선을 상향 돌파하면 골든크로스, 하향 돌파하면 데드크로스라고 합니다.',
    exampleSentence:
      '삼성전자의 5일 이동평균선이 20일 이동평균선을 위로 뚫고 올라갔다면, 단기적으로 상승 추세가 시작될 수 있습니다.',
    relatedTerms: [
      { id: 14, name: 'RSI' },
      { id: 15, name: 'MACD' },
    ],
  },
  {
    id: 14,
    name: 'RSI',
    nameEn: 'Relative Strength Index',
    category: '기술적 분석',
    difficulty: 'INTERMEDIATE',
    simpleDescription:
      '주식이 너무 많이 올랐는지(과매수) 또는 너무 많이 떨어졌는지(과매도) 알려주는 지표입니다. 0~100 사이 값입니다.',
    detailedDescription:
      'RSI(상대강도지수)는 일정 기간 동안의 상승폭과 하락폭을 비교하여 0~100 사이 값으로 나타낸 기술적 지표입니다. 일반적으로 70 이상이면 과매수(고평가), 30 이하이면 과매도(저평가)로 판단합니다.',
    exampleSentence:
      'RSI가 25라면 과매도 구간이므로, 주가가 너무 많이 내려 반등 가능성이 있다고 볼 수 있습니다.',
    relatedTerms: [
      { id: 13, name: '이동평균선' },
      { id: 15, name: 'MACD' },
    ],
  },
  {
    id: 15,
    name: 'MACD',
    nameEn: 'Moving Average Convergence Divergence',
    category: '기술적 분석',
    difficulty: 'ADVANCED',
    simpleDescription:
      '두 개의 이동평균선 차이를 이용하여 주가의 추세 전환 시점을 찾는 지표입니다.',
    detailedDescription:
      'MACD는 단기 지수이동평균(12일)에서 장기 지수이동평균(26일)을 뺀 값입니다. MACD가 시그널선(9일 EMA)을 상향 돌파하면 매수 신호, 하향 돌파하면 매도 신호로 해석됩니다.',
    exampleSentence:
      'MACD가 시그널선 위로 올라갔다면, 상승 추세로 전환되고 있을 가능성이 있습니다.',
    relatedTerms: [
      { id: 13, name: '이동평균선' },
      { id: 14, name: 'RSI' },
    ],
  },

  {
    id: 16,
    name: '기준금리',
    nameEn: 'Base Rate',
    category: '채권/금리',
    difficulty: 'BEGINNER',
    simpleDescription:
      '중앙은행이 정하는 기본 금리입니다. 모든 이자율의 기준이 되며, 경제에 큰 영향을 줍니다.',
    detailedDescription:
      '기준금리는 한국은행(또는 미국 연준)이 금융시장에서의 자금 거래 기준이 되는 금리입니다. 기준금리가 오르면 대출 이자가 오르고 예금 이자도 오르며, 주식시장에는 보통 부정적인 영향을 미칩니다.',
    exampleSentence:
      '한국은행이 기준금리를 3.5%로 동결했다는 것은, 지금 경제 상황을 유지하겠다는 의미입니다.',
    relatedTerms: [
      { id: 17, name: '국채' },
      { id: 18, name: '인플레이션' },
    ],
  },
  {
    id: 17,
    name: '국채',
    nameEn: 'Government Bond',
    category: '채권/금리',
    difficulty: 'INTERMEDIATE',
    simpleDescription:
      '정부가 돈을 빌리기 위해 발행하는 채권입니다. 가장 안전한 투자 상품 중 하나입니다.',
    detailedDescription:
      '국채(Government Bond)는 국가가 재정 자금을 조달하기 위해 발행하는 채권입니다. 국가가 원금과 이자를 보장하므로 가장 안전한 투자 자산으로 분류됩니다. 국채 금리는 시장 금리의 기준이 됩니다.',
    exampleSentence:
      '미국 10년 국채 금리가 4.5%라면, 미국 정부에 10년간 돈을 빌려주면 연 4.5%의 이자를 받을 수 있습니다.',
    relatedTerms: [
      { id: 16, name: '기준금리' },
    ],
  },

  {
    id: 18,
    name: '인플레이션',
    nameEn: 'Inflation',
    category: '거시경제',
    difficulty: 'BEGINNER',
    simpleDescription:
      '물가가 지속적으로 오르는 현상입니다. 같은 돈으로 살 수 있는 것이 줄어듭니다.',
    detailedDescription:
      '인플레이션(Inflation)은 전반적인 물가 수준이 지속적으로 상승하는 현상입니다. CPI(소비자물가지수)로 측정되며, 적정 수준의 인플레이션(2% 내외)은 경제 성장에 자연스러운 현상이지만, 과도한 인플레이션은 구매력을 떨어뜨립니다.',
    exampleSentence:
      '인플레이션이 5%라면, 작년에 10,000원이던 물건이 올해는 10,500원이 된 것입니다.',
    relatedTerms: [
      { id: 16, name: '기준금리' },
      { id: 19, name: 'GDP' },
    ],
  },
  {
    id: 19,
    name: 'GDP',
    nameEn: 'Gross Domestic Product',
    category: '거시경제',
    difficulty: 'BEGINNER',
    simpleDescription:
      '한 나라에서 일정 기간 동안 만들어진 모든 상품과 서비스의 총 가치입니다. 나라의 경제 규모를 나타냅니다.',
    detailedDescription:
      'GDP(국내총생산)는 일정 기간 동안 한 국가의 영토 내에서 생산된 모든 최종 재화와 서비스의 시장 가치 합계입니다. GDP 성장률은 경제 성장 속도를 나타내며, 마이너스 성장이 2분기 이상 지속되면 경기 침체(Recession)로 판단합니다.',
    exampleSentence:
      '한국의 GDP 성장률이 2.5%라면, 우리나라 경제가 전년 대비 2.5% 성장했다는 뜻입니다.',
    relatedTerms: [
      { id: 18, name: '인플레이션' },
      { id: 20, name: 'CPI' },
    ],
  },
  {
    id: 20,
    name: 'CPI',
    nameEn: 'Consumer Price Index',
    category: '거시경제',
    difficulty: 'INTERMEDIATE',
    simpleDescription:
      '소비자가 구입하는 상품과 서비스의 가격 변동을 측정하는 지수입니다. 물가 변동을 파악하는 핵심 지표입니다.',
    detailedDescription:
      'CPI(소비자물가지수)는 가구에서 소비하는 대표적인 상품과 서비스의 가격 변동을 종합적으로 측정하는 지표입니다. 기준 시점을 100으로 놓고, 현재 물가 수준을 비교합니다. CPI 상승률이 곧 인플레이션율입니다.',
    exampleSentence:
      'CPI가 전년 대비 3.5% 상승했다면, 소비자가 체감하는 물가가 1년 전보다 3.5% 올랐다는 의미입니다.',
    relatedTerms: [
      { id: 18, name: '인플레이션' },
      { id: 19, name: 'GDP' },
    ],
  },

  {
    id: 21,
    name: 'ETF',
    nameEn: 'Exchange-Traded Fund',
    category: '파생상품',
    difficulty: 'BEGINNER',
    simpleDescription:
      '여러 주식을 한 번에 묶어서 거래할 수 있는 펀드입니다. 주식처럼 실시간으로 사고팔 수 있습니다.',
    detailedDescription:
      'ETF(상장지수펀드)는 특정 지수, 섹터, 원자재 등을 추종하는 펀드로, 주식시장에 상장되어 주식처럼 거래됩니다. 개별 주식보다 분산투자 효과가 있으며, 운용 수수료가 일반 펀드보다 낮은 것이 장점입니다.',
    exampleSentence:
      'KODEX 200 ETF를 사면, 코스피200 지수에 포함된 200개 기업에 한 번에 투자하는 효과가 있습니다.',
    relatedTerms: [
      { id: 1, name: '주식' },
      { id: 3, name: '시가총액' },
    ],
  },
  {
    id: 22,
    name: '가치투자',
    nameEn: 'Value Investing',
    category: '주식 기초',
    difficulty: 'INTERMEDIATE',
    simpleDescription:
      '기업의 실제 가치보다 주가가 낮을 때 매수하여, 장기적으로 가치가 인정받을 때까지 기다리는 투자 전략입니다.',
    detailedDescription:
      '가치투자는 기업의 내재 가치(실적, 자산, 성장 잠재력 등)를 분석하여 시장에서 저평가된 주식을 매수하고 장기 보유하는 투자 전략입니다. 워런 버핏이 대표적인 가치투자자입니다. PER, PBR 등 밸류에이션 지표가 핵심 도구입니다.',
    exampleSentence:
      'PBR이 0.5배인 회사를 발견했다면, 회사의 자산 가치 대비 주가가 절반이므로 가치투자 관점에서 매력적일 수 있습니다.',
    relatedTerms: [
      { id: 8, name: 'PER' },
      { id: 9, name: 'PBR' },
    ],
  },
  {
    id: 23,
    name: '나스닥',
    nameEn: 'NASDAQ',
    category: '주식 기초',
    difficulty: 'BEGINNER',
    simpleDescription:
      '미국의 기술주 중심 주식 거래소입니다. 애플, 마이크로소프트, 엔비디아 등 IT 기업들이 상장되어 있습니다.',
    detailedDescription:
      'NASDAQ(나스닥)은 미국의 전자주식거래소로, 기술/성장주 중심입니다. 1971년에 설립되었으며, 뉴욕증권거래소(NYSE)와 함께 미국을 대표하는 증권거래소입니다. 나스닥 종합지수는 나스닥에 상장된 모든 종목의 시가총액 가중 지수입니다.',
    exampleSentence:
      '나스닥 지수가 16,000을 돌파했다는 것은, 미국 기술주들이 전반적으로 좋은 성과를 내고 있다는 의미입니다.',
    relatedTerms: [
      { id: 1, name: '주식' },
      { id: 3, name: '시가총액' },
    ],
  },
];
