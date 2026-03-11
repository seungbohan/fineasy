'use client';

import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';

export interface WatchlistBriefing {
  briefing: string;
  generatedAt: string;
}

export function useWatchlistBriefing(enabled: boolean) {
  return useQuery<WatchlistBriefing>({
    queryKey: ['watchlist', 'briefing'],
    queryFn: () => apiClient.get<WatchlistBriefing>('/watchlist/briefing'),
    enabled,
    staleTime: 4 * 60 * 60 * 1000,
  });
}
