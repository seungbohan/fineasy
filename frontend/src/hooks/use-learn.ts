'use client';

import { useQuery } from '@tanstack/react-query';
import { LearnArticle } from '@/types';
import { apiClient } from '@/lib/api-client';

interface LearnArticleApiResponse {
  id: number;
  title: string;
  content: string;
  category: 'BASICS' | 'NEWS_READING' | 'CHART_ANALYSIS';
  difficulty: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  estimatedReadMinutes: number;
  completed: boolean;
}

function toLearnArticle(raw: LearnArticleApiResponse): LearnArticle {
  return {
    id: raw.id,
    title: raw.title,
    content: raw.content,
    category: raw.category,
    difficulty: raw.difficulty,
    estimatedReadMinutes: raw.estimatedReadMinutes,
    isCompleted: raw.completed,
  };
}

export function useLearnArticles() {
  return useQuery<LearnArticle[]>({
    queryKey: ['learn', 'articles'],
    queryFn: async () => {
      const res = await apiClient.get<LearnArticleApiResponse[]>('/learn/articles');
      return res.map(toLearnArticle);
    },
  });
}

export function useLearnArticle(articleId: number) {
  return useQuery<LearnArticle | undefined>({
    queryKey: ['learn', 'articles', articleId],
    queryFn: async () => {
      const res = await apiClient.get<LearnArticleApiResponse>(
        `/learn/articles/${articleId}`
      );
      return toLearnArticle(res);
    },
    enabled: articleId > 0,
  });
}
