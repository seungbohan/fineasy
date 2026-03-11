'use client';

import { useState } from 'react';
import { ExternalLink, Sparkles, TrendingUp, Tag, AlertCircle, Newspaper } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
} from '@/components/ui/sheet';
import { Skeleton } from '@/components/ui/skeleton';
import { SentimentBadge } from '@/components/shared/sentiment-badge';
import { NewsListSkeleton } from '@/components/shared/loading-skeleton';
import { useNews, useNewsAnalysis } from '@/hooks/use-news';
import { formatRelativeTime } from '@/lib/format';
import { cn } from '@/lib/utils';

type SentimentFilter = 'ALL' | 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';

const SENTIMENT_FILTERS: { value: SentimentFilter; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'POSITIVE', label: '긍정' },
  { value: 'NEGATIVE', label: '부정' },
  { value: 'NEUTRAL', label: '중립' },
];

function getSentimentAccentColor(sentiment: string): string {
  switch (sentiment) {
    case 'POSITIVE': return 'bg-red-400';
    case 'NEGATIVE': return 'bg-blue-400';
    default: return 'bg-gray-300';
  }
}

function getSentimentHeroBg(sentiment: string): string {
  switch (sentiment) {
    case 'POSITIVE': return 'bg-gradient-to-br from-red-50/60 to-white';
    case 'NEGATIVE': return 'bg-gradient-to-br from-blue-50/60 to-white';
    default: return 'bg-white';
  }
}

function AnalysisSkeleton() {
  return (
    <div className="space-y-5 px-4 py-2">
      <div className="space-y-2">
        <Skeleton className="h-4 w-20" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-3/4" />
      </div>
      <div className="space-y-2">
        <Skeleton className="h-4 w-24" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-5/6" />
        <Skeleton className="h-4 w-2/3" />
      </div>
      <div className="space-y-2">
        <Skeleton className="h-4 w-20" />
        <div className="flex gap-2">
          <Skeleton className="h-7 w-16 rounded-full" />
          <Skeleton className="h-7 w-20 rounded-full" />
          <Skeleton className="h-7 w-14 rounded-full" />
        </div>
      </div>
      <div className="space-y-2">
        <Skeleton className="h-4 w-24" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-4/5" />
      </div>
    </div>
  );
}

function AnalysisError() {
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-4 py-12">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100">
        <AlertCircle className="h-6 w-6 text-gray-400" />
      </div>
      <p className="text-[13px] text-gray-500">분석을 불러올 수 없습니다</p>
      <p className="text-[11px] text-gray-400">잠시 후 다시 시도해 주세요</p>
    </div>
  );
}

export default function NewsPage() {
  const [sentiment, setSentiment] = useState<SentimentFilter>('ALL');
  const [page, setPage] = useState(1);
  const pageSize = 10;

  const [selectedNewsId, setSelectedNewsId] = useState<number | null>(null);
  const [sheetOpen, setSheetOpen] = useState(false);

  const { data, isLoading } = useNews({
    sentiment: sentiment === 'ALL' ? undefined : sentiment,
    page,
    size: pageSize,
  });

  const {
    data: analysisData,
    isLoading: isAnalysisLoading,
    isError: isAnalysisError,
  } = useNewsAnalysis(selectedNewsId);

  const totalPages = data ? Math.ceil(data.total / pageSize) : 1;

  const handleArticleClick = (newsId: number) => {
    setSelectedNewsId(newsId);
    setSheetOpen(true);
  };

  const handleSheetOpenChange = (open: boolean) => {
    setSheetOpen(open);
    if (!open) {
      setSelectedNewsId(null);
    }
  };

  const heroArticle = data?.items?.[0];
  const restArticles = data?.items?.slice(1) ?? [];

  return (
    <div className="mx-auto max-w-screen-xl p-4 pb-8 md:p-6 md:pb-10 space-y-5">

      <div className="flex items-center gap-2">
        <Newspaper className="h-5 w-5 text-[#3182F6]" />
        <h1 className="text-2xl font-bold text-gray-900">금융 뉴스</h1>
      </div>

      <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-1">
        {SENTIMENT_FILTERS.map((filter) => (
          <button
            key={filter.value}
            className={cn(
              'shrink-0 rounded-full px-4 py-2 text-[13px] font-semibold transition-colors',
              sentiment === filter.value
                ? 'bg-gray-900 text-white'
                : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
            )}
            onClick={() => {
              setSentiment(filter.value);
              setPage(1);
            }}
          >
            {filter.label}
          </button>
        ))}
      </div>

      {isLoading ? (
        <NewsListSkeleton count={6} />
      ) : data && data.items.length > 0 ? (
        <div className="space-y-3">

          {heroArticle && (
            <button
              type="button"
              onClick={() => handleArticleClick(heroArticle.id)}
              className="w-full text-left"
            >
              <Card className={cn(
                'rounded-2xl border-0 shadow-none overflow-hidden transition-all duration-300 hover:shadow-[0_4px_16px_rgba(0,0,0,0.06)] relative',
                getSentimentHeroBg(heroArticle.sentiment)
              )}>

                <div className="absolute -left-8 -bottom-8 h-32 w-32 rounded-full bg-blue-100/30 blur-2xl pointer-events-none" />
                <CardContent className="p-5 md:p-6 relative z-10">
                  <div className="flex items-center gap-2 mb-3">
                    <span className="rounded-full bg-[#3182F6] px-2.5 py-0.5 text-[10px] font-semibold text-white">
                      주요 뉴스
                    </span>
                    <SentimentBadge sentiment={heroArticle.sentiment} />
                  </div>
                  <h2 className="text-lg md:text-xl font-bold text-gray-900 leading-snug line-clamp-2">
                    {heroArticle.title}
                  </h2>
                  <div className="mt-3 flex items-center gap-2 flex-wrap">
                    <span className="text-xs text-gray-500">
                      {heroArticle.sourceName}
                    </span>
                    <span className="text-xs text-gray-400">
                      {formatRelativeTime(heroArticle.publishedAt)}
                    </span>
                    {heroArticle.stockCodes && heroArticle.stockCodes.length > 0 && (
                      <span className="text-[11px] text-[#3182F6] font-semibold">
                        {heroArticle.stockCodes.join(', ')}
                      </span>
                    )}
                  </div>
                  <div className="mt-3 flex items-center gap-1.5 text-xs text-[#3182F6] font-semibold">
                    <Sparkles className="h-3.5 w-3.5" />
                    AI 분석 보기
                  </div>
                </CardContent>
              </Card>
            </button>
          )}

          {restArticles.length > 0 && (
            <Card className="rounded-2xl border-0 bg-white shadow-none overflow-hidden">
              <CardContent className="divide-y divide-gray-50 p-0">
                {restArticles.map((article) => (
                  <button
                    key={article.id}
                    type="button"
                    className="group flex w-full items-stretch text-left transition-colors hover:bg-gray-50/50"
                    onClick={() => handleArticleClick(article.id)}
                  >

                    <div className={cn(
                      'w-1 group-hover:w-1.5 shrink-0 transition-all duration-200',
                      getSentimentAccentColor(article.sentiment)
                    )} />
                    <div className="flex flex-1 items-start gap-3 px-4 py-4">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-semibold text-gray-900 leading-snug line-clamp-2">
                          {article.title}
                        </p>
                        <div className="mt-2 flex items-center gap-2 flex-wrap">
                          <SentimentBadge sentiment={article.sentiment} />
                          <span className="text-[11px] text-gray-500">
                            {article.sourceName}
                          </span>
                          <span className="text-[11px] text-gray-400">
                            {formatRelativeTime(article.publishedAt)}
                          </span>
                          {article.stockCodes && article.stockCodes.length > 0 && (
                            <span className="text-[11px] text-[#3182F6] font-semibold">
                              {article.stockCodes.join(', ')}
                            </span>
                          )}
                        </div>
                      </div>
                      <Sparkles className="mt-1 h-4 w-4 shrink-0 text-gray-200 transition-colors duration-200 group-hover:text-[#3182F6]" />
                    </div>
                  </button>
                ))}
              </CardContent>
            </Card>
          )}
        </div>
      ) : (
        <div className="py-16 text-center text-[13px] text-gray-400">
          뉴스가 없습니다
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3">
          <button
            disabled={page <= 1}
            onClick={() => setPage((p) => p - 1)}
            className={cn(
              'rounded-full px-4 py-2 text-[13px] font-semibold transition-colors',
              page <= 1
                ? 'bg-gray-50 text-gray-300 cursor-not-allowed'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            )}
          >
            이전
          </button>
          <span className="text-sm text-gray-500 tabular-nums">
            {page} / {totalPages}
          </span>
          <button
            disabled={page >= totalPages}
            onClick={() => setPage((p) => p + 1)}
            className={cn(
              'rounded-full px-4 py-2 text-[13px] font-semibold transition-colors',
              page >= totalPages
                ? 'bg-gray-50 text-gray-300 cursor-not-allowed'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            )}
          >
            다음
          </button>
        </div>
      )}

      <Sheet open={sheetOpen} onOpenChange={handleSheetOpenChange}>
        <SheetContent
          side="bottom"
          className="mx-auto max-w-screen-md rounded-t-3xl max-h-[85vh] overflow-y-auto"
        >

          <div className="flex justify-center pt-3 pb-1">
            <div className="h-1.5 w-12 rounded-full bg-gray-200" />
          </div>

          <SheetHeader className="px-5 pb-0">
            <SheetTitle className="text-base font-bold text-gray-900 leading-snug pr-6">
              {analysisData?.title ?? '뉴스 AI 분석'}
            </SheetTitle>
            <SheetDescription asChild>
              <div className="flex items-center gap-2 flex-wrap pt-1">
                {analysisData && (
                  <>
                    <SentimentBadge sentiment={analysisData.analysis.sentiment} />
                    <span className="text-[11px] text-gray-400">
                      {analysisData.source}
                    </span>
                    <span className="text-[11px] text-gray-400">
                      {formatRelativeTime(analysisData.publishedAt)}
                    </span>
                  </>
                )}
              </div>
            </SheetDescription>
          </SheetHeader>

          {isAnalysisLoading ? (
            <AnalysisSkeleton />
          ) : isAnalysisError ? (
            <AnalysisError />
          ) : analysisData ? (
            <div className="space-y-5 px-5 py-3">

              <section>
                <div className="flex items-center gap-1.5 mb-2">
                  <Sparkles className="h-3.5 w-3.5 text-[#3182F6] animate-bounce" style={{ animationDuration: '2s' }} />
                  <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wide">
                    AI 요약
                  </h3>
                </div>
                <p className="text-sm text-gray-800 leading-relaxed bg-blue-50/50 rounded-2xl p-4">
                  {analysisData.analysis.summary}
                </p>
              </section>

              <section>
                <div className="flex items-center gap-1.5 mb-2">
                  <TrendingUp className="h-3.5 w-3.5 text-[#3182F6]" />
                  <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wide">
                    시장 영향
                  </h3>
                </div>
                <p className="text-sm text-gray-700 leading-relaxed">
                  {analysisData.analysis.marketImpact}
                </p>
              </section>

              {analysisData.analysis.relatedStocks.length > 0 && (
                <section>
                  <div className="flex items-center gap-1.5 mb-2">
                    <Tag className="h-3.5 w-3.5 text-[#3182F6]" />
                    <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wide">
                      관련 종목
                    </h3>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {analysisData.analysis.relatedStocks.map((stock) => (
                      <span
                        key={stock}
                        className="inline-flex items-center rounded-full bg-gray-100 px-3 py-1.5 text-xs font-semibold text-gray-700 transition-colors hover:bg-gray-200"
                      >
                        {stock}
                      </span>
                    ))}
                  </div>
                </section>
              )}

            </div>
          ) : null}

          {analysisData && !isAnalysisLoading && !isAnalysisError && (
            <SheetFooter className="px-5 pb-6">
              <a
                href={analysisData.originalUrl}
                target="_blank"
                rel="noopener noreferrer"
                className={cn(
                  'flex w-full items-center justify-center gap-2',
                  'rounded-2xl bg-[#3182F6] px-4 py-3.5',
                  'text-sm font-bold text-white',
                  'transition-all duration-200 hover:bg-[#1B6BF3] active:scale-[0.98]',
                )}
              >
                <ExternalLink className="h-4 w-4" />
                원문 보기
              </a>
            </SheetFooter>
          )}
        </SheetContent>
      </Sheet>
    </div>
  );
}
