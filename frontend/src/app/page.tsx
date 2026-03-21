'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import {
  ChevronRight,
  Heart,
  Plus,
  Sparkles,
  AlertCircle,
  TrendingUp,
  TrendingDown,
  Minus,
  BarChart3,
  Newspaper,
  Lightbulb,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import { Sparkline } from '@/components/shared/sparkline';
import { MarketIndexSkeleton } from '@/components/shared/loading-skeleton';
import { useMarketIndices, useMarketSummary } from '@/hooks/use-market';
import { useLatestIndicators } from '@/hooks/use-macro';
import { useWatchlistBriefing } from '@/hooks/use-watchlist';
import { useWatchlistStore, WatchlistItem } from '@/stores/watchlist-store';
import { useAuthStore } from '@/stores/auth-store';
import { apiClient } from '@/lib/api-client';
import {
  formatPrice,
  formatChangeRate,
  formatIndexValue,
  getPriceColorClass,
  getPriceArrow,
} from '@/lib/format';

export default function HomePage() {
  const { isAuthenticated } = useAuthStore();
  const { watchlist } = useWatchlistStore();

  const { data: serverItems = [] } = useQuery<WatchlistItem[]>({
    queryKey: ['watchlist', 'server'],
    queryFn: () => apiClient.get<WatchlistItem[]>('/watchlist'),
    enabled: isAuthenticated,
  });

  const watchlistCodes = isAuthenticated
    ? serverItems.map((item) => item.stockCode)
    : watchlist;

  const hasWatchlist = isAuthenticated && watchlistCodes.length > 0;

  return (
    <div className="mx-auto max-w-screen-xl space-y-6 p-4 pb-8 md:p-6 md:pb-10">
      <MarketIndicesSection />
      <MacroIndicatorsSection />
      <MarketSummarySection />
      {hasWatchlist && (
        <WatchlistBriefingSection watchlistCodes={watchlistCodes} />
      )}
      <WatchlistSection />
    </div>
  );
}

function MarketIndicesSection() {
  const { data: indices, isLoading } = useMarketIndices();

  return (
    <section aria-label="시장 지수">
      <h2 className="mb-3 text-lg font-bold text-gray-900">
        오늘의 시장
      </h2>
      {isLoading ? (
        <div className="flex gap-3 overflow-x-auto scrollbar-hide pb-1 md:grid md:grid-cols-5 md:overflow-x-visible">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="min-w-[140px] md:min-w-0">
              <MarketIndexSkeleton />
            </div>
          ))}
        </div>
      ) : (
        <div className="flex gap-3 overflow-x-auto scrollbar-hide pb-1 md:grid md:grid-cols-5 md:overflow-x-visible">
          {indices?.map((index) => {
            const colorClass = getPriceColorClass(index.changeAmount);
            const sparkColor =
              index.changeAmount > 0
                ? '#F04452'
                : index.changeAmount < 0
                ? '#3182F6'
                : '#8B95A1';

            return (
              <Card
                key={index.code}
                className="min-w-[140px] shrink-0 gap-2 rounded-2xl border-0 bg-white p-4 shadow-none card-hover cursor-default md:min-w-0"
              >
                <CardContent className="flex items-center justify-between p-0">
                  <div className="min-w-0">
                    <p className="text-xs font-semibold text-gray-500">
                      {index.name}
                    </p>
                    <p className="mt-1 text-lg font-bold tabular-nums text-gray-900">
                      {formatIndexValue(index.currentValue)}
                    </p>
                    <p className={`mt-0.5 text-xs tabular-nums font-medium ${colorClass}`}>
                      {getPriceArrow(index.changeAmount)}{' '}
                      {formatChangeRate(index.changeRate)}
                    </p>
                  </div>
                  <Sparkline
                    data={index.sparklineData}
                    color={sparkColor}
                    width={64}
                    height={28}
                  />
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </section>
  );
}

const KEY_INDICATOR_CODES = [
  'KR_BASE_RATE',
  'US_FED_FUNDS_RATE',
  'KR_USD_KRW',
  'US_10Y_TREASURY',
  'US_VIX',
  'GOLD',
  'WTI',
  'US_DXY',
];

function MacroIndicatorsSection() {
  const { data: indicators, isLoading } = useLatestIndicators();

  const keyIndicators = indicators?.filter((ind) =>
    KEY_INDICATOR_CODES.includes(ind.indicatorCode)
  );

  return (
    <section aria-label="거시경제 지표">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">
          거시경제 지표
        </h2>
        <Link
          href="/macro"
          className="flex items-center text-xs font-semibold text-gray-400 hover:text-gray-600"
        >
          전체보기
          <ChevronRight className="ml-0.5 h-3 w-3" />
        </Link>
      </div>
      {isLoading ? (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <div
              key={i}
              className="space-y-2 rounded-2xl bg-white p-4"
            >
              <Skeleton className="h-3 w-16" />
              <Skeleton className="h-5 w-20" />
              <Skeleton className="h-2.5 w-12" />
            </div>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
          {keyIndicators?.map((indicator) => {
            const barColor =
              indicator.changeRate != null && indicator.changeRate > 0
                ? 'bg-[#F04452]'
                : indicator.changeRate != null && indicator.changeRate < 0
                  ? 'bg-[#3182F6]'
                  : 'bg-gray-200';
            return (
              <Card
                key={indicator.id}
                className="gap-1 rounded-2xl border-0 bg-white p-0 shadow-none card-hover cursor-default overflow-hidden"
              >
                <div className="flex items-stretch">
                  <div className={cn('w-1 shrink-0 rounded-l-2xl', barColor)} />
                  <CardContent className="p-4 flex-1">
                    <p className="text-xs font-semibold text-gray-500">
                      {indicator.indicatorName}
                    </p>
                    <p className="mt-1 text-lg font-bold tabular-nums text-gray-900">
                      {indicator.value.toLocaleString('ko-KR', {
                        minimumFractionDigits: indicator.unit === '%' ? 1 : 1,
                        maximumFractionDigits: indicator.unit === '%' ? 2 : 1,
                      })}
                      <span className="ml-1 text-xs font-normal text-gray-400">
                        {indicator.unit}
                      </span>
                    </p>
                    {indicator.changeRate != null && (
                      <p
                        className={`mt-0.5 text-xs font-semibold tabular-nums ${
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
                    <p className="mt-0.5 text-[11px] text-gray-400">
                      {indicator.source}
                    </p>
                  </CardContent>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </section>
  );
}

function MarketSummarySection() {
  const { data, isLoading } = useMarketSummary();

  const sentimentConfig = {
    POSITIVE: { icon: TrendingUp, color: 'text-red-500', bg: 'bg-red-50', label: '상승' },
    NEGATIVE: { icon: TrendingDown, color: 'text-blue-500', bg: 'bg-blue-50', label: '하락' },
    NEUTRAL: { icon: Minus, color: 'text-gray-500', bg: 'bg-gray-100', label: '보합' },
  };

  const sentiment = sentimentConfig[(data?.sentiment as keyof typeof sentimentConfig) || 'NEUTRAL'] || sentimentConfig.NEUTRAL;
  const SentimentIcon = sentiment.icon;
  const hasStructuredData = data?.overview || data?.macro || data?.news;

  return (
    <section aria-label="시장 요약">
      <Card className="rounded-2xl border-0 bg-gradient-to-br from-blue-50/40 via-white to-white shadow-none overflow-hidden relative">
        <CardContent className="p-5 relative z-10">
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-[#3182F6] animate-subtle-pulse" />
              <span className="text-[15px] font-bold text-gray-900">
                오늘의 시장 요약
              </span>
              <span className="rounded-full bg-blue-50 px-2.5 py-0.5 text-[11px] font-semibold ai-shimmer">
                AI
              </span>
            </div>
            {!isLoading && data?.sentimentLabel && (
              <div className={cn('flex items-center gap-1.5 rounded-full px-3 py-1', sentiment.bg)}>
                <SentimentIcon className={cn('h-3.5 w-3.5', sentiment.color)} />
                <span className={cn('text-xs font-semibold', sentiment.color)}>
                  {data.sentimentLabel}
                </span>
              </div>
            )}
          </div>
          {isLoading ? (
            <div className="space-y-3">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-5/6" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-3/4" />
            </div>
          ) : hasStructuredData ? (
            <div className="space-y-3">
              {data?.overview && (
                <div className="flex gap-2.5">
                  <SentimentIcon className={cn('h-4 w-4 mt-0.5 shrink-0', sentiment.color)} />
                  <p className="text-sm leading-relaxed text-gray-800 font-medium">
                    {data.overview}
                  </p>
                </div>
              )}
              {data?.macro && (
                <div className="flex gap-2.5">
                  <BarChart3 className="h-4 w-4 mt-0.5 shrink-0 text-amber-500" />
                  <p className="text-sm leading-relaxed text-gray-600">
                    {data.macro}
                  </p>
                </div>
              )}
              {data?.news && (
                <div className="flex gap-2.5">
                  <Newspaper className="h-4 w-4 mt-0.5 shrink-0 text-purple-500" />
                  <p className="text-sm leading-relaxed text-gray-600">
                    {data.news}
                  </p>
                </div>
              )}
              {data?.tip && (
                <div className="mt-2 flex gap-2.5 rounded-xl bg-amber-50/80 p-3">
                  <Lightbulb className="h-4 w-4 mt-0.5 shrink-0 text-amber-500" />
                  <p className="text-[13px] leading-relaxed text-amber-800 font-medium">
                    {data.tip}
                  </p>
                </div>
              )}
            </div>
          ) : (
            <p className="text-sm leading-relaxed text-gray-700">
              {data?.summary}
            </p>
          )}
          <Link
            href="/analysis"
            className="mt-4 inline-flex items-center text-[13px] font-semibold text-[#3182F6] hover:underline"
          >
            자세한 분석 보기
            <ChevronRight className="ml-0.5 h-3 w-3" />
          </Link>
        </CardContent>
      </Card>
    </section>
  );
}

function WatchlistBriefingSection({ watchlistCodes }: { watchlistCodes: string[] }) {
  const {
    data: briefing,
    isLoading: isBriefingLoading,
    isError: isBriefingError,
  } = useWatchlistBriefing(watchlistCodes.length > 0);

  return (
    <section aria-label="AI 관심종목 브리핑">
      <WatchlistBriefingCard
        briefing={briefing}
        isLoading={isBriefingLoading}
        isError={isBriefingError}
      />
    </section>
  );
}

function WatchlistBriefingCard({
  briefing,
  isLoading,
  isError,
}: {
  briefing: { briefing: string; generatedAt: string } | undefined;
  isLoading: boolean;
  isError: boolean;
}) {
  if (isError) {
    return (
      <Card className="rounded-2xl border-0 bg-gradient-to-br from-red-50/30 via-white to-white shadow-none">
        <CardContent className="p-5">
          <div className="flex items-center gap-3">
            <AlertCircle className="h-4 w-4 text-gray-400 shrink-0" />
            <p className="text-sm text-gray-500">
              AI 브리핑을 불러올 수 없습니다
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="rounded-2xl border-0 bg-gradient-to-br from-blue-50/40 via-white to-white shadow-none overflow-hidden relative">
      <CardContent className="p-5 relative z-10">
        <div className="mb-3 flex items-center gap-2">
          <Sparkles className="h-4 w-4 text-[#3182F6] animate-subtle-pulse" />
          <span className="text-[15px] font-bold text-gray-900">
            AI 관심종목 브리핑
          </span>
          <span className="rounded-full bg-blue-50 px-2.5 py-0.5 text-[11px] font-semibold ai-shimmer">
            AI
          </span>
        </div>
        {isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-5/6" />
            <Skeleton className="h-4 w-3/4" />
          </div>
        ) : briefing ? (
          <>
            <p className="text-sm leading-relaxed text-gray-700">
              {briefing.briefing}
            </p>
            <p className="mt-3 text-[10px] text-gray-400">
              이 분석은 AI가 생성한 참고 자료이며, 투자 권유가 아닙니다
            </p>
          </>
        ) : (
          <p className="text-sm text-gray-400">
            브리핑 데이터가 없습니다
          </p>
        )}
      </CardContent>
    </Card>
  );
}

function WatchlistSection() {
  const { isAuthenticated } = useAuthStore();
  const { watchlist, removeStock } = useWatchlistStore();

  interface StockPriceResponse {
    stockCode: string;
    stockName: string;
    currentPrice: number;
    changeAmount: number;
    changeRate: number;
    volume: number;
  }

  const { data: serverItems = [], refetch } = useQuery<WatchlistItem[]>({
    queryKey: ['watchlist', 'server'],
    queryFn: () => apiClient.get<WatchlistItem[]>('/watchlist'),
    enabled: isAuthenticated,
  });

  const watchlistCodes = isAuthenticated
    ? serverItems.map((item) => item.stockCode)
    : watchlist;

  const { data: watchedStocks = [] } = useQuery<{
    stockCode: string;
    stockName: string;
    currentPrice: number;
    changeAmount: number;
    changeRate: number;
    currency: 'KRW' | 'USD';
  }[]>({
    queryKey: ['watchlist', 'prices', watchlistCodes],
    queryFn: async () => {
      if (watchlistCodes.length === 0) return [];
      const results = await Promise.allSettled(
        watchlistCodes.map((code) =>
          apiClient.get<StockPriceResponse>(`/stocks/${code}/price`)
        )
      );
      return results
        .filter((r): r is PromiseFulfilledResult<StockPriceResponse> => r.status === 'fulfilled')
        .map((r) => ({
          stockCode: r.value.stockCode,
          stockName: r.value.stockName,
          currentPrice: r.value.currentPrice,
          changeAmount: r.value.changeAmount,
          changeRate: r.value.changeRate,
          currency: /^[A-Z]+$/.test(r.value.stockCode) ? 'USD' as const : 'KRW' as const,
        }));
    },
    enabled: watchlistCodes.length > 0,
  });

  return (
    <section aria-label="관심 종목">
      <Card className="rounded-2xl border-0 bg-white shadow-none">
        <CardContent className="p-5">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-bold text-gray-900">
              내 관심 종목
            </h2>
            <span className="text-xs text-gray-400 tabular-nums">
              {watchedStocks.length}/10
            </span>
          </div>

          {!isAuthenticated ? (
            <div className="flex flex-col items-center gap-4 py-12 text-center">
              <div className="flex h-14 w-14 items-center justify-center rounded-full border-2 border-dashed border-gray-200">
                <Heart className="h-6 w-6 text-gray-300" />
              </div>
              <div>
                <p className="text-[15px] font-bold text-gray-700">
                  로그인하고 관심 종목을 관리하세요
                </p>
                <p className="mt-1.5 text-[13px] text-gray-500 leading-relaxed">
                  로그인하면 관심 종목을 등록하고<br />실시간 시세를 확인할 수 있어요
                </p>
              </div>
              <Link href="/login">
                <Button
                  variant="outline"
                  size="sm"
                  className="text-[13px] rounded-full px-5 border-[#3182F6] text-[#3182F6] hover:bg-blue-50 transition-colors"
                >
                  로그인하기
                </Button>
              </Link>
            </div>
          ) : watchedStocks.length === 0 ? (
            <div className="flex flex-col items-center gap-4 py-12 text-center">
              <div className="flex h-14 w-14 items-center justify-center rounded-full border-2 border-dashed border-gray-200">
                <Plus className="h-6 w-6 text-gray-300" />
              </div>
              <div>
                <p className="text-[15px] font-bold text-gray-700">
                  관심 종목을 추가해보세요
                </p>
                <p className="mt-1.5 text-[13px] text-gray-500 leading-relaxed">
                  종목 상세 페이지에서 하트를 눌러<br />나만의 관심 종목을 관리하세요
                </p>
              </div>
              <Link href="/stocks">
                <Button
                  variant="outline"
                  size="sm"
                  className="text-[13px] rounded-full px-5 border-[#3182F6] text-[#3182F6] hover:bg-blue-50 transition-colors"
                >
                  종목 둘러보기
                </Button>
              </Link>
            </div>
          ) : (
            <div className="space-y-0">

              <div className="flex items-center py-2 border-b border-gray-50">
                <span className="flex-1 text-[11px] text-gray-400">종목명</span>
                <span className="w-24 text-right text-[11px] text-gray-400">현재가</span>
                <span className="w-20 text-right text-[11px] text-gray-400">등락률</span>
                <span className="w-8" />
              </div>
              {watchedStocks.map((stock) => (
                <div
                  key={stock.stockCode}
                  className="flex items-center py-3"
                >
                  <Link
                    href={`/stocks/${stock.stockCode}`}
                    className="flex-1 min-w-0 flex items-center"
                  >
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">
                        {stock.stockName}
                      </p>
                      <p className="text-[11px] text-gray-400">{stock.stockCode}</p>
                    </div>
                    <span className="w-24 text-right text-sm tabular-nums text-gray-900">
                      {stock.currency === 'USD'
                        ? formatPrice(stock.currentPrice, 'USD')
                        : formatPrice(stock.currentPrice)
                      }
                    </span>
                    <span
                      className={cn(
                        'w-20 text-right text-sm font-medium tabular-nums',
                        getPriceColorClass(stock.changeRate)
                      )}
                    >
                      {formatChangeRate(stock.changeRate)}
                    </span>
                  </Link>
                  <div className="w-8 flex justify-end">
                    <button
                      onClick={async (e) => {
                        e.preventDefault();
                        await removeStock(stock.stockCode);
                        if (isAuthenticated) refetch();
                      }}
                      className="text-red-400 hover:text-red-500 transition-colors"
                      aria-label={`${stock.stockName} 관심 종목 해제`}
                    >
                      <Heart className="h-4 w-4 fill-current" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </section>
  );
}
