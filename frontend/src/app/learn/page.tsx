'use client';

import Link from 'next/link';
import {
  Clock,
  BookOpen,
  BarChart3,
  Newspaper,
  Search,
  Globe,
  Cpu,
  Shield,
  Battery,
  Pill,
  Landmark,
  Flame,
  ChevronRight,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { DifficultyBadge } from '@/components/shared/difficulty-badge';
import { useLearnArticles } from '@/hooks/use-learn';

const CATEGORY_CONFIG = {
  BASICS: {
    icon: BookOpen,
    label: '투자 기초',
    color: 'text-green-600',
    bg: 'bg-green-50',
  },
  NEWS_READING: {
    icon: Newspaper,
    label: '뉴스 읽는 법',
    color: 'text-blue-600',
    bg: 'bg-blue-50',
  },
  CHART_ANALYSIS: {
    icon: BarChart3,
    label: '차트 분석',
    color: 'text-purple-600',
    bg: 'bg-purple-50',
  },
  VALUE_ANALYSIS: {
    icon: Search,
    label: '가치 분석',
    color: 'text-amber-600',
    bg: 'bg-amber-50',
  },
  MACRO_ECONOMY: {
    icon: Globe,
    label: '거시경제',
    color: 'text-teal-600',
    bg: 'bg-teal-50',
  },
};

export default function LearnPage() {
  const { data: articles, isLoading } = useLearnArticles();

  const grouped = (articles || []).reduce<
    Record<string, typeof articles>
  >((acc, article) => {
    if (!article) return acc;
    if (!acc[article.category]) acc[article.category] = [];
    acc[article.category]!.push(article);
    return acc;
  }, {});

  const categoryOrder: Array<keyof typeof CATEGORY_CONFIG> = [
    'BASICS',
    'VALUE_ANALYSIS',
    'MACRO_ECONOMY',
    'NEWS_READING',
    'CHART_ANALYSIS',
  ];

  const SECTOR_PREVIEWS = [
    { slug: 'semiconductor', name: '반도체', icon: Cpu, color: 'text-blue-600', bg: 'bg-blue-50' },
    { slug: 'defense', name: '방산', icon: Shield, color: 'text-slate-600', bg: 'bg-slate-100' },
    { slug: 'battery', name: '이차전지', icon: Battery, color: 'text-green-600', bg: 'bg-green-50' },
    { slug: 'bio', name: '바이오', icon: Pill, color: 'text-purple-600', bg: 'bg-purple-50' },
    { slug: 'finance', name: '금융', icon: Landmark, color: 'text-amber-600', bg: 'bg-amber-50' },
    { slug: 'energy', name: '에너지', icon: Flame, color: 'text-orange-600', bg: 'bg-orange-50' },
  ];

  return (
    <div className="mx-auto max-w-screen-xl p-4 md:p-6 space-y-4">
      <h1 className="text-xl font-bold text-gray-900">투자 학습 센터</h1>
      <p className="text-sm text-gray-500">
        투자 기초부터 가치 분석, 거시경제까지 단계별로 배워보세요.
      </p>

      {/* Sector analysis section */}
      <div>
        <div className="mb-2 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50">
              <BarChart3 className="h-4 w-4 text-indigo-600" />
            </div>
            <h2 className="text-sm font-semibold text-gray-900">섹터 분석</h2>
          </div>
          <Link
            href="/learn/sectors"
            className="flex items-center gap-0.5 text-xs text-gray-400 hover:text-[#3182F6] transition-colors"
          >
            전체보기 <ChevronRight className="h-3 w-3" />
          </Link>
        </div>
        <Card className="rounded-xl border-0 bg-white shadow-none">
          <CardContent className="p-3">
            <div className="grid grid-cols-3 gap-2 sm:grid-cols-6">
              {SECTOR_PREVIEWS.map((s) => {
                const SIcon = s.icon;
                return (
                  <Link
                    key={s.slug}
                    href={`/learn/sectors/${s.slug}`}
                    className="flex flex-col items-center gap-1.5 rounded-lg p-2.5 transition-colors hover:bg-gray-50"
                  >
                    <div className={`flex h-9 w-9 items-center justify-center rounded-lg ${s.bg}`}>
                      <SIcon className={`h-4 w-4 ${s.color}`} />
                    </div>
                    <span className="text-[11px] font-medium text-gray-700">{s.name}</span>
                  </Link>
                );
              })}
            </div>
          </CardContent>
        </Card>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <Card
              key={i}
              className="rounded-xl border-0 bg-white shadow-none animate-pulse"
            >
              <CardContent className="h-32 p-4" />
            </Card>
          ))}
        </div>
      ) : (
        categoryOrder.map((categoryKey) => {
          const config = CATEGORY_CONFIG[categoryKey];
          const categoryArticles = grouped[categoryKey];
          if (!categoryArticles || categoryArticles.length === 0) return null;

          const Icon = config.icon;

          return (
            <div key={categoryKey}>
              <div className="mb-2 flex items-center gap-2">
                <div
                  className={`flex h-7 w-7 items-center justify-center rounded-lg ${config.bg}`}
                >
                  <Icon className={`h-4 w-4 ${config.color}`} />
                </div>
                <h2 className="text-sm font-semibold text-gray-900">
                  {config.label}
                </h2>
              </div>

              <Card className="rounded-xl border-0 bg-white shadow-none">
                <CardContent className="divide-y divide-gray-100 p-0">
                  {categoryArticles.map((article) => (
                    <Link
                      key={article.id}
                      href={`/learn/${article.id}`}
                      className="flex items-center justify-between px-4 py-3.5 transition-colors hover:bg-gray-50"
                    >
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900">
                          {article.title}
                        </p>
                        <div className="mt-1.5 flex items-center gap-2">
                          <DifficultyBadge difficulty={article.difficulty} />
                          <span className="flex items-center gap-1 text-[11px] text-gray-400">
                            <Clock className="h-3 w-3" />
                            {article.estimatedReadMinutes}분
                          </span>
                        </div>
                      </div>
                      <span className="text-gray-300">&rsaquo;</span>
                    </Link>
                  ))}
                </CardContent>
              </Card>
            </div>
          );
        })
      )}
    </div>
  );
}
