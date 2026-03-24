'use client';

import { useQuery } from '@tanstack/react-query';
import { DomesticDisclosure, OverseasDisclosure } from '@/types';
import { apiClient } from '@/lib/api-client';

interface DomesticDisclosureApiResponse {
  stockCode: string;
  corpName: string;
  totalCount: number;
  disclosures: {
    receiptNumber: string;
    reportName: string;
    filerName: string;
    receiptDate: string;
    disclosureType: string;
    dartUrl: string;
  }[];
}

interface OverseasDisclosureApiResponse {
  stockCode: string;
  companyName: string;
  totalCount: number;
  filings: {
    accessionNumber: string;
    filingType: string;
    filingDate: string;
    description: string;
    secUrl: string;
  }[];
}

/**
 * Fetch DART disclosures for a domestic (KRX/KOSDAQ) stock.
 */
export function useDomesticDisclosure(stockCode: string) {
  return useQuery<DomesticDisclosure[]>({
    queryKey: ['disclosure', 'domestic', stockCode],
    queryFn: async () => {
      const res = await apiClient.get<DomesticDisclosureApiResponse>(
        `/disclosure/domestic/${stockCode}`
      );
      if (!res?.disclosures) return [];
      return res.disclosures.map((item, idx) => ({
        id: idx,
        stockCode: res.stockCode,
        title: item.reportName.trim(),
        submitter: item.filerName,
        filingDate: item.receiptDate,
        disclosureType: item.disclosureType,
        dartUrl: item.dartUrl,
      }));
    },
    enabled: !!stockCode,
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Fetch SEC EDGAR filings for an overseas (NASDAQ/NYSE) stock.
 */
export function useOverseasDisclosure(stockCode: string) {
  return useQuery<OverseasDisclosure[]>({
    queryKey: ['disclosure', 'overseas', stockCode],
    queryFn: async () => {
      const res = await apiClient.get<OverseasDisclosureApiResponse>(
        `/disclosure/overseas/${stockCode}`
      );
      if (!res?.filings) return [];
      return res.filings.map((item, idx) => ({
        id: idx,
        stockCode: res.stockCode,
        title: item.description || item.filingType,
        filingType: item.filingType,
        filingDate: item.filingDate,
        edgarUrl: item.secUrl,
      }));
    },
    enabled: !!stockCode,
    staleTime: 5 * 60 * 1000,
  });
}
