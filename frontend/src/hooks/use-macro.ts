'use client';

import { useQuery } from '@tanstack/react-query';
import { MacroIndicator } from '@/types';
import { apiClient } from '@/lib/api-client';

export function useLatestIndicators() {
  return useQuery<MacroIndicator[]>({
    queryKey: ['macro', 'indicators'],
    queryFn: () => apiClient.get<MacroIndicator[]>('/macro/indicators'),
    staleTime: 5 * 60 * 1000,
  });
}

export function useIndicatorsByCategory(category: string) {
  return useQuery<MacroIndicator[]>({
    queryKey: ['macro', 'indicators', 'category', category],
    queryFn: () =>
      apiClient.get<MacroIndicator[]>(
        `/macro/indicators/category/${category}`
      ),
    staleTime: 5 * 60 * 1000,
    enabled: !!category,
  });
}
