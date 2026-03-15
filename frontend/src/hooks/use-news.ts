'use client';

import { useQuery, useInfiniteQuery } from '@tanstack/react-query';
import {
  NewsArticle,
  NewsAnalysisResponse,
  NewNewsCount,
  StockNewsSummary,
  SentimentTrendResponse,
} from '@/types';
import { apiClient } from '@/lib/api-client';

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface NewsArticleApiResponse {
  id: number;
  title: string;
  content: string;
  originalUrl: string;
  sourceName: string;
  publishedAt: string;
  sentiment: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  sentimentScore: number;
  taggedStocks?: { stockCode: string; stockName: string }[];
  isBreaking?: boolean;
}

function toNewsArticle(raw: NewsArticleApiResponse): NewsArticle {
  return {
    id: raw.id,
    title: raw.title,
    sourceName: raw.sourceName,
    publishedAt: raw.publishedAt,
    sentiment: raw.sentiment,
    sentimentScore: raw.sentimentScore,
    originalUrl: raw.originalUrl,
    taggedStocks: raw.taggedStocks,
    isBreaking: raw.isBreaking,
  };
}

export function useNews(options?: {
  sentiment?: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  stockCode?: string;
  page?: number;
  size?: number;
}) {
  const { sentiment, stockCode, page = 1, size = 10 } = options || {};

  return useQuery<{ items: NewsArticle[]; total: number }>({
    queryKey: ['news', sentiment, stockCode, page, size],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', String(size));
      if (sentiment) params.set('sentiment', sentiment);
      if (stockCode) params.set('stockCode', stockCode);

      const res = await apiClient.get<PageResponse<NewsArticleApiResponse>>(
        `/news?${params.toString()}`
      );

      return {
        items: res.content.map(toNewsArticle),
        total: res.totalElements,
      };
    },
  });
}

export function useStockNews(stockCode: string) {
  return useQuery<NewsArticle[]>({
    queryKey: ['news', 'stock', stockCode],
    queryFn: async () => {
      const res = await apiClient.get<NewsArticleApiResponse[]>(
        `/stocks/${stockCode}/news`
      );
      return res.map(toNewsArticle);
    },
    enabled: !!stockCode,
    staleTime: 60 * 1000,
    retry: 3,
    retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 10000),
  });
}

export function useWatchlistNews(stockCodes: string[], size = 8) {
  return useQuery<WatchlistNewsArticle[]>({
    queryKey: ['news', 'watchlist', stockCodes],
    queryFn: async () => {
      const res = await apiClient.get<WatchlistNewsArticleApiResponse[]>(
        `/news/watchlist?stockCodes=${stockCodes.join(',')}&size=${size}`
      );
      return res.map((raw) => ({
        ...toNewsArticle(raw),
        stockCodes: raw.stockCodes ?? [],
        stockNames: raw.stockNames ?? [],
      }));
    },
    enabled: stockCodes.length > 0,
    staleTime: 2 * 60 * 1000,
  });
}

interface WatchlistNewsArticleApiResponse extends NewsArticleApiResponse {
  stockCodes?: string[];
  stockNames?: string[];
}

export interface WatchlistNewsArticle extends NewsArticle {
  stockCodes: string[];
  stockNames: string[];
}

export function useNewsAnalysis(newsId: number | null) {
  return useQuery<NewsAnalysisResponse>({
    queryKey: ['news', 'analysis', newsId],
    queryFn: async () => {
      return apiClient.get<NewsAnalysisResponse>(
        `/news/${newsId}/analysis`
      );
    },
    enabled: newsId !== null,
    staleTime: 1000 * 60 * 30,
  });
}

/**
 * Infinite scroll hook for news page.
 * Fetches paginated news with cursor-based pagination.
 */
export function useInfiniteNews(options?: {
  sentiment?: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  stockCode?: string;
  size?: number;
}) {
  const { sentiment, stockCode, size = 10 } = options || {};

  return useInfiniteQuery<{ items: NewsArticle[]; total: number }>({
    queryKey: ['news', 'infinite', sentiment, stockCode, size],
    queryFn: async ({ pageParam }) => {
      const params = new URLSearchParams();
      params.set('page', String(pageParam));
      params.set('size', String(size));
      if (sentiment) params.set('sentiment', sentiment);
      if (stockCode) params.set('stockCode', stockCode);

      const res = await apiClient.get<PageResponse<NewsArticleApiResponse>>(
        `/news?${params.toString()}`
      );

      return {
        items: res.content.map(toNewsArticle),
        total: res.totalElements,
      };
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      const totalFetched = allPages.reduce((sum, p) => sum + p.items.length, 0);
      if (totalFetched >= lastPage.total) return undefined;
      return allPages.length + 1;
    },
  });
}

/**
 * Hook to check for new news articles since a given timestamp.
 * Polls every 30 seconds.
 */
export function useNewNewsCount(since: string | null) {
  return useQuery<NewNewsCount>({
    queryKey: ['news', 'latestCount', since],
    queryFn: () =>
      apiClient.get<NewNewsCount>(
        `/news/latest-count?since=${encodeURIComponent(since!)}`
      ),
    enabled: !!since,
    refetchInterval: 30 * 1000,
    staleTime: 10 * 1000,
  });
}

/**
 * Fetch watchlist-filtered news for "my stocks" tab.
 * Endpoint: GET /api/v1/news/watchlist
 */
export function useWatchlistFilteredNews(
  stockCodes: string[],
  page = 1,
  size = 10
) {
  return useQuery<{ items: NewsArticle[]; total: number }>({
    queryKey: ['news', 'watchlistFiltered', stockCodes, page, size],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.set('stockCodes', stockCodes.join(','));
      params.set('page', String(page));
      params.set('size', String(size));
      const res = await apiClient.get<PageResponse<NewsArticleApiResponse>>(
        `/news/watchlist?${params.toString()}`
      );
      return {
        items: res.content.map(toNewsArticle),
        total: res.totalElements,
      };
    },
    enabled: stockCodes.length > 0,
    staleTime: 2 * 60 * 1000,
  });
}

/**
 * AI-generated stock news summary for a specific stock.
 * Endpoint: GET /api/v1/news/stock-summary/{stockCode}
 */
export function useStockNewsSummary(stockCode: string) {
  return useQuery<StockNewsSummary>({
    queryKey: ['news', 'stockSummary', stockCode],
    queryFn: () =>
      apiClient.get<StockNewsSummary>(`/news/stock-summary/${stockCode}`),
    enabled: !!stockCode,
    staleTime: 30 * 60 * 1000,
  });
}

/**
 * Sentiment trend for a stock over N days.
 * Endpoint: GET /api/v1/news/sentiment-trend/{stockCode}?days=30
 */
export function useSentimentTrend(stockCode: string, days = 30) {
  return useQuery<SentimentTrendResponse>({
    queryKey: ['news', 'sentimentTrend', stockCode, days],
    queryFn: () =>
      apiClient.get<SentimentTrendResponse>(
        `/news/sentiment-trend/${stockCode}?days=${days}`
      ),
    enabled: !!stockCode,
    staleTime: 60 * 60 * 1000,
  });
}
