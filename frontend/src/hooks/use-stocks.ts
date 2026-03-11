'use client';

import { useQuery, useInfiniteQuery } from '@tanstack/react-query';
import { Stock, StockCandle, StockChartData, DartFundamentals, MultiYearFundamentals, SectorComparison } from '@/types';
import { apiClient } from '@/lib/api-client';
import { getCurrencyFromMarket } from '@/lib/format';

interface StockBasicResponse {
  id: number;
  stockCode: string;
  stockName: string;
  market: 'KRX' | 'KOSDAQ' | 'NASDAQ' | 'NYSE';
  sector: string;
}

interface StockPriceResponse {
  stockCode: string;
  stockName: string;
  currentPrice: number;
  changeAmount: number;
  changeRate: number;
  volume: number;
  tradeDate: string;
}

interface StockFinancialsResponse {
  stockCode: string;
  stockName: string;
  marketCap: number | null;
  sharesOutstanding: number;
  per: number | null;
  pbr: number | null;
  eps: number | null;
  dividendYield: number | null;
  high52Week: number | null;
  low52Week: number | null;
}

interface StockChartApiResponse {
  stockCode: string;
  stockName: string;
  period: string;
  candles: StockCandle[];
  indicators: Record<string, (number | null)[]>;
}

function formatMarketCap(value: number | null): string {
  if (!value) return '-';
  const trillion = 1_000_000_000_000;
  const billion = 100_000_000;
  if (value >= trillion) return `${(value / trillion).toFixed(1)}조`;
  if (value >= billion) return `${(value / billion).toFixed(0)}억`;
  return value.toLocaleString();
}

async function fetchStockWithPrice(basic: StockBasicResponse): Promise<Stock> {
  const [price, financials] = await Promise.allSettled([
    apiClient.get<StockPriceResponse>(`/stocks/${basic.stockCode}/price`),
    apiClient.get<StockFinancialsResponse>(`/stocks/${basic.stockCode}/financials`),
  ]);

  const priceData = price.status === 'fulfilled' ? price.value : null;
  const finData = financials.status === 'fulfilled' ? financials.value : null;

  return {
    stockCode: basic.stockCode,
    stockName: basic.stockName,
    market: basic.market,
    sector: basic.sector,
    currentPrice: priceData?.currentPrice ?? 0,
    changeAmount: priceData?.changeAmount ?? 0,
    changeRate: priceData?.changeRate ?? 0,
    volume: priceData?.volume ?? 0,
    tradingValue: 0,
    marketCap: formatMarketCap(finData?.marketCap ?? null),
    per: finData?.per ?? 0,
    pbr: finData?.pbr ?? 0,
    eps: finData?.eps ?? 0,
    dividendYield: finData?.dividendYield ?? 0,
    high52w: finData?.high52Week ?? 0,
    low52w: finData?.low52Week ?? 0,
    currency: getCurrencyFromMarket(basic.market),
  };
}

export function usePopularStocks(
  tab: 'volume' | 'gainers' | 'losers',
  region: 'domestic' | 'overseas' = 'domestic'
) {
  return useQuery<Stock[]>({
    queryKey: ['stocks', 'popular', region, tab],
    queryFn: async () => {
      const basics = await apiClient.get<StockBasicResponse[]>(
        `/stocks/popular?region=${region}&size=20`
      );
      const stocks = await Promise.all(basics.map(fetchStockWithPrice));

      switch (tab) {
        case 'gainers':
          return [...stocks].sort((a, b) => b.changeRate - a.changeRate);
        case 'losers':
          return [...stocks].sort((a, b) => a.changeRate - b.changeRate);
        default:
          return stocks;
      }
    },
    staleTime: 15000,
  });
}

const REALTIME_POLL_INTERVAL_MS = 10_000;

export function useStockDetail(stockCode: string) {
  return useQuery<Stock | undefined>({
    queryKey: ['stocks', stockCode],
    queryFn: async () => {
      const basic = await apiClient.get<StockBasicResponse>(`/stocks/${stockCode}`);
      return fetchStockWithPrice(basic);
    },
    enabled: !!stockCode,
    refetchInterval: REALTIME_POLL_INTERVAL_MS,
  });
}

export function useStockChart(
  stockCode: string,
  period: '1D' | '1W' | '1M' | '3M' | '1Y' | 'ALL'
) {
  return useQuery<StockChartData>({
    queryKey: ['stocks', stockCode, 'chart', period],
    queryFn: async () => {
      const res = await apiClient.get<StockChartApiResponse>(
        `/stocks/${stockCode}/chart?period=${period}`
      );
      return {
        candles: res.candles,
        indicators: res.indicators ?? {},
      };
    },
    enabled: !!stockCode,
    refetchInterval: period === '1D' ? REALTIME_POLL_INTERVAL_MS : false,
  });
}

export function useStockSearch(query: string) {
  return useQuery<Stock[]>({
    queryKey: ['stocks', 'search', query],
    queryFn: async () => {
      if (!query.trim()) return [];
      const basics = await apiClient.get<StockBasicResponse[]>(
        `/stocks/search?q=${encodeURIComponent(query)}`
      );
      return basics.map((b) => ({
        stockCode: b.stockCode,
        stockName: b.stockName,
        market: b.market,
        sector: b.sector,
        currentPrice: 0,
        changeAmount: 0,
        changeRate: 0,
        volume: 0,
        tradingValue: 0,
        marketCap: '-',
        per: 0,
        pbr: 0,
        eps: 0,
        dividendYield: 0,
        high52w: 0,
        low52w: 0,
        currency: getCurrencyFromMarket(b.market),
      }));
    },
    enabled: query.length > 0,
  });
}

const FUNDAMENTALS_STALE_TIME_MS = 86_400_000;

export function useStockFundamentals(stockCode: string) {
  return useQuery<DartFundamentals | null>({
    queryKey: ['stocks', stockCode, 'fundamentals'],
    queryFn: async () => {
      try {
        return await apiClient.get<DartFundamentals>(
          `/stocks/${stockCode}/fundamentals`
        );
      } catch {
        return null;
      }
    },
    enabled: !!stockCode,
    staleTime: FUNDAMENTALS_STALE_TIME_MS,
  });
}

export function useStockFundamentalsHistory(stockCode: string) {
  return useQuery<MultiYearFundamentals | null>({
    queryKey: ['stocks', stockCode, 'fundamentals', 'history'],
    queryFn: async () => {
      try {
        return await apiClient.get<MultiYearFundamentals>(
          `/stocks/${stockCode}/fundamentals/history`
        );
      } catch {
        return null;
      }
    },
    enabled: !!stockCode,
    staleTime: FUNDAMENTALS_STALE_TIME_MS,
    retry: 3,
    retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 10000),
  });
}

export function useSectorComparison(stockCode: string) {
  return useQuery<SectorComparison | null>({
    queryKey: ['stocks', stockCode, 'sector-comparison'],
    queryFn: async () => {
      try {
        return await apiClient.get<SectorComparison>(
          `/stocks/${stockCode}/sector-comparison`
        );
      } catch {
        return null;
      }
    },
    enabled: !!stockCode,
    staleTime: FUNDAMENTALS_STALE_TIME_MS,
  });
}

const STOCK_LIST_PAGE_SIZE = 30;

export function useStockList(region: 'domestic' | 'overseas') {
  return useInfiniteQuery<StockBasicResponse[], Error>({
    queryKey: ['stocks', 'list', region],
    queryFn: async ({ pageParam }) => {
      return apiClient.get<StockBasicResponse[]>(
        `/stocks/popular?region=${region}&page=${pageParam}&size=${STOCK_LIST_PAGE_SIZE}`
      );
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage, _allPages, lastPageParam) => {
      if (lastPage.length < STOCK_LIST_PAGE_SIZE) return undefined;
      return (lastPageParam as number) + 1;
    },
  });
}
