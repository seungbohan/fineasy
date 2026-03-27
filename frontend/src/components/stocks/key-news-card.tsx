'use client';

import { useState } from 'react';
import { Zap, ChevronDown, ChevronUp } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { formatRelativeTime } from '@/lib/format';
import { useStockKeyNews, type KeyNewsArticle } from '@/hooks/use-news';

/** Impact type label mapping and color classes */
const IMPACT_TYPE_CONFIG: Record<
  KeyNewsArticle['impactType'],
  { label: string; className: string }
> = {
  DIRECT: { label: '직접', className: 'bg-red-50 text-red-600' },
  SUPPLY_CHAIN: { label: '공급망', className: 'bg-amber-50 text-amber-600' },
  COMPETITOR: { label: '경쟁사', className: 'bg-purple-50 text-purple-600' },
  INDIRECT: { label: '업종', className: 'bg-blue-50 text-[#3182F6]' },
};

/** Impact direction dot color: follows Korean market convention (red=positive, blue=negative) */
function getDirectionDotClass(
  direction: KeyNewsArticle['impactDirection']
): string {
  switch (direction) {
    case 'POSITIVE':
      return 'bg-red-400';
    case 'NEGATIVE':
      return 'bg-blue-400';
    default:
      return 'bg-gray-300';
  }
}

/** Number of items shown before "more" toggle */
const INITIAL_COUNT = 5;

interface KeyNewsCardProps {
  stockCode: string;
  onNewsClick: (newsId: number) => void;
}

/** Card displaying key news that directly impacts a stock's revenue/earnings. */
export function KeyNewsCard({ stockCode, onNewsClick }: KeyNewsCardProps) {
  const { data: articles, isLoading, isError } = useStockKeyNews(stockCode);
  const [expanded, setExpanded] = useState(false);

  /* ---- Loading skeleton ---- */
  if (isLoading) {
    return (
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <div className="mb-3 flex items-center gap-2">
            <Skeleton className="h-5 w-5 rounded" />
            <Skeleton className="h-4 w-20" />
          </div>
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="flex items-start gap-2 py-2">
                <Skeleton className="mt-1.5 h-2 w-2 shrink-0 rounded-full" />
                <div className="flex-1 space-y-1.5">
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-3 w-1/3" />
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  /* ---- Error or empty state ---- */
  if (isError || !articles || articles.length === 0) {
    return null;
  }

  const visibleArticles = expanded
    ? articles
    : articles.slice(0, INITIAL_COUNT);
  const hasMore = articles.length > INITIAL_COUNT;

  return (
    <Card className="rounded-xl border-0 bg-white shadow-none">
      <CardContent className="p-4">
        {/* Header */}
        <div className="mb-3 flex items-center gap-2">
          <Zap className="h-4.5 w-4.5 text-amber-500" />
          <h2 className="text-[15px] font-bold text-gray-900">핵심 뉴스</h2>
          <span className="rounded-full bg-amber-50 px-2.5 py-0.5 text-[11px] font-semibold text-amber-600">
            {articles.length}건
          </span>
        </div>

        {/* News list */}
        <div className="divide-y divide-gray-50">
          {visibleArticles.map((article) => {
            const typeConfig = IMPACT_TYPE_CONFIG[article.impactType];
            const dotClass = getDirectionDotClass(article.impactDirection);

            return (
              <button
                key={article.id}
                type="button"
                className="group flex w-full items-start gap-2 py-3 text-left transition-colors hover:bg-gray-50 -mx-1 px-1 rounded-lg"
                onClick={() => onNewsClick(article.id)}
              >
                {/* Direction dot */}
                <span
                  className={`mt-2 h-2 w-2 shrink-0 rounded-full ${dotClass}`}
                />

                <div className="flex-1 min-w-0">
                  {/* Impact type label */}
                  <span
                    className={`inline-block rounded px-1.5 py-0.5 text-[10px] font-semibold leading-none mb-1 ${typeConfig.className}`}
                  >
                    {typeConfig.label}
                  </span>

                  {/* Title */}
                  <p className="text-sm font-medium text-gray-900 line-clamp-2 leading-snug">
                    {article.title}
                  </p>

                  {/* Source + time */}
                  <div className="mt-1.5 flex items-center gap-2">
                    <span className="text-[11px] text-gray-500">
                      {article.sourceName}
                    </span>
                    <span className="text-[11px] text-gray-400">
                      {formatRelativeTime(article.publishedAt)}
                    </span>
                  </div>
                </div>
              </button>
            );
          })}
        </div>

        {/* "Show more" toggle */}
        {hasMore && (
          <button
            type="button"
            className="mt-2 flex w-full items-center justify-center gap-1 rounded-lg py-2 text-[13px] font-medium text-gray-500 transition-colors hover:bg-gray-50 hover:text-gray-700"
            onClick={() => setExpanded((prev) => !prev)}
          >
            {expanded ? (
              <>
                접기 <ChevronUp className="h-3.5 w-3.5" />
              </>
            ) : (
              <>
                {articles.length - INITIAL_COUNT}건 더보기{' '}
                <ChevronDown className="h-3.5 w-3.5" />
              </>
            )}
          </button>
        )}
      </CardContent>
    </Card>
  );
}
