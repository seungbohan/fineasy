'use client';

import { useQuery } from '@tanstack/react-query';
import { AnalysisReport, Prediction } from '@/types';
import { apiClient } from '@/lib/api-client';

export function useAnalysisReport(stockCode: string) {
  return useQuery<AnalysisReport | undefined>({
    queryKey: ['analysis', stockCode, 'report'],
    queryFn: async () => {
      return apiClient.get<AnalysisReport>(`/analysis/${stockCode}/report`);
    },
    enabled: !!stockCode,
    staleTime: 6 * 60 * 60 * 1000,
  });
}

export function usePrediction(stockCode: string, period: '1D' | '1W' = '1D') {
  return useQuery<Prediction | undefined>({
    queryKey: ['analysis', stockCode, 'prediction', period],
    queryFn: async () => {
      return apiClient.get<Prediction>(
        `/analysis/${stockCode}/prediction?period=${period}`
      );
    },
    enabled: !!stockCode,
    staleTime: 6 * 60 * 60 * 1000,
  });
}
