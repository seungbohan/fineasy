import { create } from 'zustand';
import { apiClient } from '@/lib/api-client';

export interface WatchlistItem {
  id: number;
  stockCode: string;
  stockName: string;
  currentPrice: number;
  changeAmount: number;
  changeRate: number;
  addedAt: string;
}

interface WatchlistState {
  watchlist: string[];
  loading: boolean;
  addStock: (stockCode: string) => Promise<void>;
  removeStock: (stockCode: string) => Promise<void>;
  isWatched: (stockCode: string) => boolean;
  fetchWatchlist: () => Promise<void>;
  hydrate: () => void;
}

const MAX_LOCAL_WATCHLIST = 10;
const STORAGE_KEY = 'fineasy_watchlist';

function isLoggedIn(): boolean {
  return typeof window !== 'undefined' && !!localStorage.getItem('accessToken');
}

export const useWatchlistStore = create<WatchlistState>((set, get) => ({
  watchlist: [],
  loading: false,

  fetchWatchlist: async () => {
    if (!isLoggedIn()) return;
    set({ loading: true });
    try {
      const items = await apiClient.get<WatchlistItem[]>('/watchlist');
      set({ watchlist: items.map((item) => item.stockCode) });
    } catch {
      get().hydrate();
    } finally {
      set({ loading: false });
    }
  },

  addStock: async (stockCode: string) => {
    if (!isLoggedIn()) return;
    const current = get().watchlist;
    if (current.includes(stockCode)) return;
    if (current.length >= MAX_LOCAL_WATCHLIST) return;

    const updated = [...current, stockCode];
    set({ watchlist: updated });

    try {
      await apiClient.post(`/watchlist/${stockCode}`);
    } catch {
      set({ watchlist: current });
    }
  },

  removeStock: async (stockCode: string) => {
    if (!isLoggedIn()) return;
    const current = get().watchlist;
    const updated = current.filter((code) => code !== stockCode);

    set({ watchlist: updated });

    try {
      await apiClient.delete(`/watchlist/${stockCode}`);
    } catch {
      set({ watchlist: current });
    }
  },

  isWatched: (stockCode: string) => {
    return get().watchlist.includes(stockCode);
  },

  hydrate: () => {
    if (typeof window === 'undefined') return;
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        const parsed = JSON.parse(stored) as string[];
        set({ watchlist: parsed });
      } catch {
        localStorage.removeItem(STORAGE_KEY);
      }
    }
  },
}));
