'use client';

import { useQuery } from '@tanstack/react-query';
import { MarketIndex, Stock } from '@/types';
import { apiClient } from '@/lib/api-client';

interface MarketIndexApiResponse {
  indices: MarketIndex[];
  updatedAt: string;
}

interface MarketSummaryApiResponse {
  summary: string;
  generatedAt: string;
}

export function useMarketIndices() {
  return useQuery<MarketIndex[]>({
    queryKey: ['market', 'indices'],
    queryFn: async () => {
      const res = await apiClient.get<MarketIndexApiResponse>('/market/indices');
      return res.indices;
    },
    refetchInterval: 15000,
    staleTime: 10000,
  });
}

export function useMarketSummary() {
  return useQuery<{ summary: string; updatedAt: string }>({
    queryKey: ['market', 'summary'],
    queryFn: async () => {
      const res = await apiClient.get<MarketSummaryApiResponse>('/market/summary');
      return {
        summary: res.summary,
        updatedAt: res.generatedAt,
      };
    },
    staleTime: 6 * 60 * 60 * 1000,
  });
}

interface RankedStock {
  rank: number;
  stockCode: string;
  stockName: string;
  currentPrice: number;
  changeAmount: number;
  changeRate: number;
  volume: number;
  tradingValue: number;
}

interface StockRankingApiResponse {
  type: string;
  stocks: RankedStock[];
  updatedAt: string;
}

export function useStockRanking(
  type: 'gainers' | 'losers' | 'volume' | 'trading_value',
  size: number = 20,
  region: 'domestic' | 'overseas' = 'domestic',
  enabled: boolean = true
) {
  return useQuery<Stock[]>({
    queryKey: ['market', 'ranking', type, size, region],
    queryFn: async () => {
      const res = await apiClient.get<StockRankingApiResponse>(
        `/market/ranking?type=${type}&size=${size}&region=${region}`
      );
      return res.stocks.map((s) => ({
        stockCode: s.stockCode,
        stockName: s.stockName,
        market: (region === 'overseas' ? 'NASDAQ' : 'KRX') as Stock['market'],
        sector: '',
        currentPrice: s.currentPrice,
        changeAmount: s.changeAmount,
        changeRate: s.changeRate,
        volume: s.volume,
        tradingValue: s.tradingValue ?? 0,
        marketCap: '-',
        per: 0,
        pbr: 0,
        eps: 0,
        dividendYield: 0,
        high52w: 0,
        low52w: 0,
        currency: region === 'overseas' ? 'USD' as const : 'KRW' as const,
      }));
    },
    enabled,
    staleTime: 15000,
  });
}
