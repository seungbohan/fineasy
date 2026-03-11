'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState, useEffect, type ReactNode } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useWatchlistStore } from '@/stores/watchlist-store';
import { useSearchStore } from '@/stores/search-store';

function StoreHydration() {
  const hydrateAuth = useAuthStore((s) => s.hydrate);
  const hydrateWatchlist = useWatchlistStore((s) => s.hydrate);
  const hydrateSearch = useSearchStore((s) => s.hydrate);

  useEffect(() => {
    hydrateAuth();
    hydrateWatchlist();
    hydrateSearch();
  }, [hydrateAuth, hydrateWatchlist, hydrateSearch]);

  return null;
}

export function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30 * 1000,
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      <StoreHydration />
      {children}
    </QueryClientProvider>
  );
}
