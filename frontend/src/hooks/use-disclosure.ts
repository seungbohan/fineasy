'use client';

import { useQuery } from '@tanstack/react-query';
import { DomesticDisclosure, OverseasDisclosure } from '@/types';
import { apiClient } from '@/lib/api-client';

/**
 * Fetch DART disclosures for a domestic (KRX/KOSDAQ) stock.
 * Endpoint: GET /api/v1/disclosure/domestic/{stockCode}
 */
export function useDomesticDisclosure(stockCode: string) {
  return useQuery<DomesticDisclosure[]>({
    queryKey: ['disclosure', 'domestic', stockCode],
    queryFn: async () => {
      const res = await apiClient.get<DomesticDisclosure[]>(
        `/disclosure/domestic/${stockCode}`
      );
      return Array.isArray(res) ? res : [];
    },
    enabled: !!stockCode,
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Fetch SEC EDGAR filings for an overseas (NASDAQ/NYSE) stock.
 * Endpoint: GET /api/v1/disclosure/overseas/{stockCode}
 */
export function useOverseasDisclosure(stockCode: string) {
  return useQuery<OverseasDisclosure[]>({
    queryKey: ['disclosure', 'overseas', stockCode],
    queryFn: async () => {
      const res = await apiClient.get<OverseasDisclosure[]>(
        `/disclosure/overseas/${stockCode}`
      );
      return Array.isArray(res) ? res : [];
    },
    enabled: !!stockCode,
    staleTime: 5 * 60 * 1000,
  });
}
