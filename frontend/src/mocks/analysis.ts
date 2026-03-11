import { AnalysisReport, Prediction } from '@/types';

export const MOCK_ANALYSIS_REPORTS: Record<string, AnalysisReport> = {
  '005930': {
    stockCode: '005930',
    generatedAt: '2026-02-20T09:00:00Z',
    summary: '삼성전자는 단기 상승 신호를 보이고 있습니다.',
    description:
      '20일 이동평균선을 돌파한 상태로, HBM 수요 증가에 대한 기대감이 주가를 지지하고 있습니다. 외국인 순매수가 3일 연속 이어지고 있어 수급적으로도 긍정적입니다.',
    keyPoints: [
      '20일 이동평균선 상향 돌파 - 단기 상승 추세 전환',
      '외국인 3일 연속 순매수, 거래량 평균 대비 120% 수준',
      'HBM 수요 증가에 대한 기대감이 주가를 지지',
    ],
    disclaimer:
      '이 분석은 AI가 생성한 참고 자료이며, 투자 권유가 아닙니다. 실제 투자 판단은 전문가와 상담 후 결정하시기 바랍니다.',
  },
  '000660': {
    stockCode: '000660',
    generatedAt: '2026-02-20T09:00:00Z',
    summary: 'SK하이닉스는 조정 구간에 진입한 것으로 보입니다.',
    description:
      '최근 5거래일 연속 상승 후 차익 실현 매물이 출현하고 있습니다. 다만 HBM 수주 확대에 대한 기대감이 하방을 지지하고 있어 큰 폭의 하락은 제한적일 것으로 보입니다.',
    keyPoints: [
      'HBM3E 수주 확대 뉴스가 하방 지지',
      '최근 5거래일 연속 상승 후 차익 실현 매물 출현',
      '상승 모멘텀 둔화 조짐',
    ],
    disclaimer:
      '이 분석은 AI가 생성한 참고 자료이며, 투자 권유가 아닙니다.',
  },
};

export const MOCK_PREDICTIONS: Record<string, Prediction> = {
  '005930': {
    stockCode: '005930',
    period: '1D',
    direction: 'UP',
    confidence: 65,
    reasons: [
      'PER 12.5배로 업종 평균 15배 대비 저평가 상태',
      '배당수익률 3.2%로 안정적 주주환원 정책',
      '최근 뉴스 감성 긍정적 (0.72)',
    ],
    disclaimer:
      '이 예측은 AI 모델 기반이며 투자 권유가 아닙니다. 실제 투자 시 전문가 상담을 권장합니다.',
    generatedAt: '2026-02-20T09:00:00Z',
    valuation: 'UNDERVALUED',
  },
  '000660': {
    stockCode: '000660',
    period: '1D',
    direction: 'SIDEWAYS',
    confidence: 55,
    reasons: [
      'PBR 1.8배로 업종 평균 수준의 적정 가격대',
      'EPS 성장률 안정적이나 업종 내 경쟁 심화',
      '부채비율 양호하나 거시경제 불확실성 존재',
    ],
    disclaimer:
      '이 예측은 AI 모델 기반이며 투자 권유가 아닙니다.',
    generatedAt: '2026-02-20T09:00:00Z',
    valuation: 'FAIR',
  },
};
