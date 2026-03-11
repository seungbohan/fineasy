'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { useLatestIndicators, useIndicatorsByCategory } from '@/hooks/use-macro';
import { MacroIndicator } from '@/types';

const CATEGORIES = [
  { key: 'ALL_KR', label: '한국경제' },
  { key: 'POLICY', label: '정책' },
  { key: 'ECONOMY', label: '경제지표' },
  { key: 'FINANCIAL_MARKET', label: '금융시장' },
  { key: 'LIQUIDITY', label: '유동성' },
  { key: 'COMMODITY', label: '원자재' },
  { key: 'FOREX', label: '환율' },
] as const;

const KR_INDICATOR_CODES = [
  'KR_BASE_RATE',
  'KR_USD_KRW',
  'KR_CPI',
  'WTI',
  'GOLD',
  'KOSPI',
  'KOSDAQ',
];

type CategoryKey = (typeof CATEGORIES)[number]['key'];

export default function MacroPage() {
  const [activeTab, setActiveTab] = useState<CategoryKey>('ALL_KR');

  return (
    <div className="mx-auto max-w-screen-xl p-4 md:p-6">

      <div className="mb-4 flex items-center gap-3">
        <Link
          href="/"
          className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-gray-700 transition-colors"
          aria-label="홈으로 돌아가기"
        >
          <ArrowLeft className="h-5 w-5" />
        </Link>
        <div>
          <h1 className="text-lg font-bold text-gray-900">거시경제 지표</h1>
          <p className="text-xs text-gray-500">
            FRED &middot; ECOS &middot; 한국은행 데이터 기반
          </p>
        </div>
      </div>

      <div className="mb-4 flex gap-1.5 overflow-x-auto pb-1 scrollbar-hide">
        {CATEGORIES.map((cat) => (
          <button
            key={cat.key}
            onClick={() => setActiveTab(cat.key)}
            className={`shrink-0 rounded-full px-3.5 py-1.5 text-xs font-medium transition-colors ${
              activeTab === cat.key
                ? 'bg-gray-900 text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            {cat.label}
          </button>
        ))}
      </div>

      <CategoryContent category={activeTab} />
    </div>
  );
}

function CategoryContent({ category }: { category: CategoryKey }) {
  if (category === 'ALL_KR') {
    return <KoreanIndicators />;
  }
  return <FredCategoryIndicators category={category} />;
}

function KoreanIndicators() {
  const { data: allIndicators, isLoading } = useLatestIndicators();

  const indicators = allIndicators?.filter((ind) =>
    KR_INDICATOR_CODES.includes(ind.indicatorCode)
  );

  return <IndicatorGrid indicators={indicators} isLoading={isLoading} />;
}

function FredCategoryIndicators({ category }: { category: string }) {
  const { data: indicators, isLoading } = useIndicatorsByCategory(category);
  return <IndicatorGrid indicators={indicators} isLoading={isLoading} />;
}

function IndicatorGrid({
  indicators,
  isLoading,
}: {
  indicators?: MacroIndicator[];
  isLoading: boolean;
}) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="space-y-2 rounded-xl bg-white p-4">
            <Skeleton className="h-3 w-20" />
            <Skeleton className="h-6 w-24" />
            <Skeleton className="h-3 w-14" />
          </div>
        ))}
      </div>
    );
  }

  if (!indicators || indicators.length === 0) {
    return (
      <div className="py-12 text-center text-sm text-gray-400">
        해당 카테고리에 데이터가 없습니다
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
      {indicators.map((indicator) => (
        <Card
          key={indicator.id}
          className="gap-1 rounded-xl border-0 bg-white p-4 shadow-none"
        >
          <CardContent className="p-0">
            <p className="text-xs font-medium text-gray-500">
              {indicator.indicatorName}
            </p>
            <p className="mt-1 text-base font-bold tabular-nums text-gray-900">
              {indicator.value.toLocaleString('ko-KR', {
                minimumFractionDigits: 1,
                maximumFractionDigits: indicator.unit === '%' ? 2 : 1,
              })}
              <span className="ml-1 text-xs font-normal text-gray-400">
                {indicator.unit}
              </span>
            </p>
            {indicator.changeRate != null && (
              <p
                className={`mt-0.5 text-[11px] font-medium tabular-nums ${
                  indicator.changeRate > 0
                    ? 'text-red-500'
                    : indicator.changeRate < 0
                      ? 'text-blue-500'
                      : 'text-gray-400'
                }`}
              >
                {indicator.changeRate > 0 ? '+' : ''}
                {indicator.changeRate.toFixed(2)}%
              </p>
            )}
            <p className="mt-1 text-[10px] text-gray-400">
              {indicator.source} &middot; {indicator.recordDate}
            </p>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
