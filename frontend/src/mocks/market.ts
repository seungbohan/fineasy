import { MarketIndex } from '@/types';

export const MOCK_MARKET_INDICES: MarketIndex[] = [
  {
    code: 'KOSPI',
    name: '코스피',
    currentValue: 2650.33,
    changeAmount: 15.22,
    changeRate: 0.58,
    sparklineData: [2620, 2630, 2628, 2645, 2650],
  },
  {
    code: 'KOSDAQ',
    name: '코스닥',
    currentValue: 865.12,
    changeAmount: -3.45,
    changeRate: -0.40,
    sparklineData: [872, 868, 870, 866, 865],
  },
  {
    code: 'NASDAQ',
    name: '나스닥',
    currentValue: 16274.94,
    changeAmount: 128.53,
    changeRate: 0.80,
    sparklineData: [16050, 16120, 16180, 16200, 16275],
  },
  {
    code: 'SP500',
    name: 'S&P 500',
    currentValue: 5088.80,
    changeAmount: 23.90,
    changeRate: 0.47,
    sparklineData: [5030, 5045, 5060, 5070, 5089],
  },
  {
    code: 'DJI',
    name: '다우존스',
    currentValue: 39131.53,
    changeAmount: -62.30,
    changeRate: -0.16,
    sparklineData: [39250, 39180, 39200, 39160, 39132],
  },
];

export const MOCK_MARKET_SUMMARY = {
  summary:
    '오늘 코스피는 반도체 대형주 강세에 힘입어 소폭 상승 마감했습니다. 삼성전자와 SK하이닉스가 HBM 수요 기대감으로 각각 1.9%, 1.3% 상승했으며, 외국인 순매수가 이어졌습니다. 반면 코스닥은 바이오 업종 약세로 소폭 하락했습니다. 미국 증시는 AI 관련주 강세로 나스닥이 0.8% 올랐으나, 다우존스는 차익 실현 매물에 소폭 하락했습니다.',
  updatedAt: '2026-02-20T09:00:00Z',
};
