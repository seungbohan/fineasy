'use client';

import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BokTerm, BokTermPage, BokTermExplanation } from '@/types';
import { apiClient } from '@/lib/api-client';

export function useBokTerms(options?: {
  keyword?: string;
  page?: number;
  size?: number;
}) {
  const { keyword, page = 0, size = 20 } = options || {};

  return useQuery<BokTermPage>({
    queryKey: ['bok-terms', keyword, page, size],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (keyword && keyword.trim()) {
        params.set('keyword', keyword);
      }
      params.set('page', String(page));
      params.set('size', String(size));
      return apiClient.get<BokTermPage>(`/bok-terms?${params.toString()}`);
    },
  });
}

export function useBokTermsAll() {
  return useQuery<BokTerm[]>({
    queryKey: ['bok-terms', 'all'],
    queryFn: async () => {
      const result = await apiClient.get<BokTermPage>(
        '/bok-terms?page=0&size=700'
      );
      return result.content;
    },
    staleTime: 1000 * 60 * 30,
  });
}

export const CHOSUNG_LIST = [
  'ㄱ', 'ㄴ', 'ㄷ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅅ',
  'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
] as const;

export const ENGLISH_GROUPS = [
  'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
  'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
] as const;

export function getChosung(char: string): string {
  const code = char.charCodeAt(0);

  if (code >= 0xac00 && code <= 0xd7a3) {
    const jamoFull = [
      'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ',
      'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ',
      'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
    ];
    const index = Math.floor((code - 0xac00) / 588);
    const jamo = jamoFull[index];
    const doubleToSingle: Record<string, string> = {
      'ㄲ': 'ㄱ', 'ㄸ': 'ㄷ', 'ㅃ': 'ㅂ', 'ㅆ': 'ㅅ', 'ㅉ': 'ㅈ',
    };
    return doubleToSingle[jamo] || jamo;
  }
  if (/[a-zA-Z]/.test(char)) return char.toUpperCase();
  return '#';
}

export function getBokCategories(terms: BokTerm[]): string[] {
  const categories = new Set<string>();
  terms.forEach((t) => {
    if (t.category) categories.add(t.category);
  });
  return Array.from(categories).sort((a, b) => a.localeCompare(b, 'ko'));
}

export function useFilteredBokTerms(
  allTerms: BokTerm[] | undefined,
  options: {
    chosung?: string;
    keyword?: string;
    category?: string;
  }
) {
  const { chosung, keyword, category } = options;

  return useMemo(() => {
    if (!allTerms) return { filtered: [], grouped: {}, chosungCounts: {} };

    const chosungCounts: Record<string, number> = {};
    allTerms.forEach((term) => {
      const ch = getChosung(term.term[0]);
      chosungCounts[ch] = (chosungCounts[ch] || 0) + 1;
    });

    let filtered = allTerms;

    if (category && category !== 'all') {
      filtered = filtered.filter((t) => t.category === category);
    }

    if (keyword && keyword.trim()) {
      const kw = keyword.trim().toLowerCase();
      filtered = filtered.filter(
        (t) =>
          t.term.toLowerCase().includes(kw) ||
          (t.englishTerm && t.englishTerm.toLowerCase().includes(kw)) ||
          t.definition.toLowerCase().includes(kw)
      );
    }

    if (chosung) {
      if (chosung === 'ENG') {

        filtered = filtered.filter((t) => /^[a-zA-Z]/.test(t.term[0]));
      } else {
        filtered = filtered.filter((t) => {
          const ch = getChosung(t.term[0]);
          return ch === chosung;
        });
      }
    }

    const grouped: Record<string, BokTerm[]> = {};
    filtered.forEach((term) => {
      const ch = getChosung(term.term[0]);
      if (!grouped[ch]) grouped[ch] = [];
      grouped[ch].push(term);
    });

    return { filtered, grouped, chosungCounts };
  }, [allTerms, chosung, keyword, category]);
}

export function useBokTermDetail(termId: number) {
  return useQuery<BokTerm>({
    queryKey: ['bok-terms', termId],
    queryFn: async () => {
      return apiClient.get<BokTerm>(`/bok-terms/${termId}`);
    },
    enabled: termId > 0,
  });
}

export function useBokTermExplanation(termId: number, enabled: boolean) {
  return useQuery<BokTermExplanation>({
    queryKey: ['bok-terms', termId, 'explanation'],
    queryFn: async () => {
      return apiClient.get<BokTermExplanation>(
        `/bok-terms/${termId}/explanation`
      );
    },
    enabled: enabled && termId > 0,
    staleTime: 1000 * 60 * 60,
    retry: 1,
  });
}
