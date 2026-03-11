'use client';

import { useQuery } from '@tanstack/react-query';
import { CryptoMarketResponse, CoinData } from '@/types';
import { apiClient } from '@/lib/api-client';

export function useCryptoList() {
  return useQuery<CryptoMarketResponse>({
    queryKey: ['crypto', 'list'],
    queryFn: () => apiClient.get<CryptoMarketResponse>('/crypto'),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCryptoDetail(symbol: string) {
  return useQuery<CryptoMarketResponse>({
    queryKey: ['crypto', 'detail', symbol],
    queryFn: () => apiClient.get<CryptoMarketResponse>(`/crypto/${symbol}`),
    enabled: !!symbol,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCryptoHistory(symbol: string) {
  return useQuery<CryptoMarketResponse>({
    queryKey: ['crypto', 'history', symbol],
    queryFn: () => apiClient.get<CryptoMarketResponse>(`/crypto/${symbol}/history`),
    enabled: !!symbol,
    staleTime: 5 * 60 * 1000,
  });
}
