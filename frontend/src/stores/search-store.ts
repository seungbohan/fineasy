import { create } from 'zustand';

const STORAGE_KEY = 'fineasy_recent_searches';
const MAX_RECENT = 5;

interface SearchState {
  recentSearches: string[];
  addSearch: (query: string) => void;
  clearSearches: () => void;
  hydrate: () => void;
}

export const useSearchStore = create<SearchState>((set, get) => ({
  recentSearches: [],

  addSearch: (query: string) => {
    const trimmed = query.trim();
    if (!trimmed) return;
    const current = get().recentSearches.filter((s) => s !== trimmed);
    const updated = [trimmed, ...current].slice(0, MAX_RECENT);
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    }
    set({ recentSearches: updated });
  },

  clearSearches: () => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem(STORAGE_KEY);
    }
    set({ recentSearches: [] });
  },

  hydrate: () => {
    if (typeof window === 'undefined') return;
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        set({ recentSearches: JSON.parse(stored) as string[] });
      } catch {
        localStorage.removeItem(STORAGE_KEY);
      }
    }
  },
}));
