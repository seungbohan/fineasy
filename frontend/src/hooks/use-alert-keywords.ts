'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { AlertKeywordsResponse } from '@/types';
import { apiClient } from '@/lib/api-client';

/**
 * Fetch the user's alert keywords.
 * Endpoint: GET /api/v1/alerts/keywords
 */
export function useAlertKeywords() {
  return useQuery<AlertKeywordsResponse>({
    queryKey: ['alerts', 'keywords'],
    queryFn: () => apiClient.get<AlertKeywordsResponse>('/alerts/keywords'),
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Add a new alert keyword.
 * Endpoint: POST /api/v1/alerts/keywords
 */
export function useAddKeyword() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (keyword: string) =>
      apiClient.post<void>('/alerts/keywords', { keyword }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts', 'keywords'] });
    },
  });
}

/**
 * Delete an alert keyword.
 * Endpoint: DELETE /api/v1/alerts/keywords/{keywordId}
 */
export function useDeleteKeyword() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (keywordId: number) =>
      apiClient.delete<void>(`/alerts/keywords/${keywordId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts', 'keywords'] });
    },
  });
}
