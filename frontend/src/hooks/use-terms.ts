'use client';

import { useQuery } from '@tanstack/react-query';
import { FinancialTerm } from '@/types';
import { apiClient } from '@/lib/api-client';

interface TermCategoryApiResponse {
  id: number;
  name: string;
  displayOrder: number;
}

export function useTermCategories() {
  return useQuery<TermCategoryApiResponse[]>({
    queryKey: ['terms', 'categories'],
    queryFn: async () => {
      return apiClient.get<TermCategoryApiResponse[]>('/terms/categories');
    },
  });
}

export function useTerms(options?: {
  category?: string;
  search?: string;
}) {
  const { category, search } = options || {};

  return useQuery<FinancialTerm[]>({
    queryKey: ['terms', category, search],
    queryFn: async () => {
      if (search && search.trim()) {
        return apiClient.get<FinancialTerm[]>(
          `/terms/search?q=${encodeURIComponent(search)}`
        );
      }

      if (category && category !== 'all') {
        return apiClient.get<FinancialTerm[]>(`/terms/category/${category}`);
      }

      return apiClient.get<FinancialTerm[]>('/terms');
    },
  });
}

export function useTermDetail(termId: number) {
  return useQuery<FinancialTerm | undefined>({
    queryKey: ['terms', termId],
    queryFn: async () => {
      return apiClient.get<FinancialTerm>(`/terms/${termId}`);
    },
    enabled: termId > 0,
  });
}
