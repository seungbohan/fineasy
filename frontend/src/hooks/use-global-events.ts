'use client';

import { useQuery } from '@tanstack/react-query';
import { GlobalEventResponse, EventType, MarketRiskResponse } from '@/types';
import { apiClient } from '@/lib/api-client';

export function useGlobalEvents(type?: EventType, page = 0, size = 20) {
  return useQuery<GlobalEventResponse>({
    queryKey: ['globalEvents', type ?? 'ALL', page, size],
    queryFn: () => {
      const params = new URLSearchParams();
      if (type) params.set('type', type);
      params.set('page', String(page));
      params.set('size', String(size));
      return apiClient.get<GlobalEventResponse>(`/global-events?${params.toString()}`);
    },
    staleTime: 5 * 60 * 1000,
  });
}

export function useGlobalEventAlerts() {
  return useQuery<GlobalEventResponse>({
    queryKey: ['globalEvents', 'alerts'],
    queryFn: () => apiClient.get<GlobalEventResponse>('/global-events/alerts'),
    staleTime: 5 * 60 * 1000,
  });
}

export function useMarketRisk() {
  return useQuery<MarketRiskResponse>({
    queryKey: ['macro', 'riskSummary'],
    queryFn: () => apiClient.get<MarketRiskResponse>('/macro/risk-summary'),
    staleTime: 5 * 60 * 1000,
  });
}
