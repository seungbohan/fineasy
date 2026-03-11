'use client';

import { useQuery } from '@tanstack/react-query';
import { NewsArticle, NewsAnalysisResponse } from '@/types';
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
