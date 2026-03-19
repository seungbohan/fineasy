'use client';

import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import Link from 'next/link';
import {
  ExternalLink,
  Sparkles,
  TrendingUp,
  Tag,
  AlertCircle,
  Newspaper,
  Bell,
  ArrowUp,
  LogIn,
  Loader2,
} from 'lucide-react';
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
import { TermHighlighter } from '@/components/shared/term-highlighter';
import { AlertKeywords } from '@/components/shared/alert-keywords';
import { useKeywordMatchedNews } from '@/hooks/use-alert-keywords';
import { NewsListSkeleton } from '@/components/shared/loading-skeleton';
import {
  useInfiniteNews,
  useNewsAnalysis,
  useNewNewsCount,
  useWatchlistFilteredNews,
} from '@/hooks/use-news';
import { useAuthStore } from '@/stores/auth-store';
import { useWatchlistStore } from '@/stores/watchlist-store';
import { formatRelativeTime } from '@/lib/format';
import { cn } from '@/lib/utils';
import type { NewsArticle } from '@/types';

type SentimentFilter = 'ALL' | 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
type NewsTab = 'all' | 'watchlist';

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

/* ── Skeleton / Error inline components ── */

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

/* ── Stock tag chip ── */

function StockTagChips({ article }: { article: NewsArticle }) {
  const tags = article.taggedStocks;
  if (!tags || tags.length === 0) return null;

  return (
    <div className="mt-1.5 flex flex-wrap gap-1">
      {tags.map((tag) => (
        <Link
          key={tag.stockCode}
          href={`/stocks/${tag.stockCode}`}
          onClick={(e) => e.stopPropagation()}
          className="rounded-full bg-gray-100 px-2 py-0.5 text-[11px] font-medium text-gray-600 transition-colors hover:bg-gray-200 hover:text-gray-800"
        >
          {tag.stockName}
        </Link>
      ))}
    </div>
  );
}

/* ── New news banner ── */

function NewNewsBanner({
  count,
  onRefresh,
}: {
  count: number;
  onRefresh: () => void;
}) {
  if (count <= 0) return null;

  return (
    <div className="animate-slide-down sticky top-16 z-20 flex justify-center px-4 py-2">
      <button
        onClick={onRefresh}
        className="flex items-center gap-2 rounded-full bg-[#3182F6] px-4 py-2 text-[13px] font-semibold text-white shadow-lg shadow-blue-200/50 transition-all hover:bg-[#1B6BF3] active:scale-95"
      >
        <ArrowUp className="h-3.5 w-3.5" />
        새 뉴스 {count}건
      </button>
    </div>
  );
}

/* ── Infinite scroll sentinel ── */

function InfiniteScrollSentinel({
  onIntersect,
  hasMore,
  isFetchingNextPage,
}: {
  onIntersect: () => void;
  hasMore: boolean;
  isFetchingNextPage: boolean;
}) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!hasMore) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          onIntersect();
        }
      },
      { rootMargin: '200px' }
    );
    if (ref.current) observer.observe(ref.current);
    return () => observer.disconnect();
  }, [hasMore, onIntersect]);

  if (!hasMore && !isFetchingNextPage) return null;

  return (
    <div ref={ref} className="flex justify-center py-6">
      {isFetchingNextPage && (
        <Loader2 className="h-5 w-5 animate-spin text-gray-400" />
      )}
    </div>
  );
}

/* ── Main Page Component ── */

export default function NewsPage() {
  const [newsTab, setNewsTab] = useState<NewsTab>('all');
  const [sentiment, setSentiment] = useState<SentimentFilter>('ALL');
  const [selectedNewsId, setSelectedNewsId] = useState<number | null>(null);
  const [sheetOpen, setSheetOpen] = useState(false);

  const { isAuthenticated } = useAuthStore();
  const { watchlist } = useWatchlistStore();

  /* Track the time of initial fetch for new-news polling */
  const [lastFetchTime, setLastFetchTime] = useState(() => new Date().toISOString());

  /* ── Infinite scroll for "all" tab ── */
  const {
    data: infiniteData,
    isLoading: isInfiniteLoading,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    resetAndRefetch,
  } = useInfiniteNews({
    sentiment: sentiment === 'ALL' ? undefined : sentiment,
  });

  const allArticles = useMemo(
    () => infiniteData?.pages.flatMap((p) => p.items) ?? [],
    [infiniteData]
  );

  /* ── Watchlist filtered news ── */
  const [watchlistPage, setWatchlistPage] = useState(0);
  const { data: watchlistData, isLoading: isWatchlistLoading } =
    useWatchlistFilteredNews(
      newsTab === 'watchlist' ? watchlist : [],
      watchlistPage
    );
  const watchlistArticles = watchlistData?.items ?? [];
  const watchlistTotalPages = watchlistData
    ? Math.ceil(watchlistData.total / 10)
    : 1;

  /* ── New news count polling ── */
  const { data: newNewsData, refetch: resetNewCount } =
    useNewNewsCount(lastFetchTime);
  const newCount = newNewsData?.count ?? 0;

  /* ── Keyword matched news ── */
  const { data: keywordNews } = useKeywordMatchedNews(isAuthenticated);

  /* ── Analysis sheet ── */
  const {
    data: analysisData,
    isLoading: isAnalysisLoading,
    isError: isAnalysisError,
  } = useNewsAnalysis(selectedNewsId);

  const handleArticleClick = (newsId: number) => {
    setSelectedNewsId(newsId);
    setSheetOpen(true);
  };

  const handleSheetOpenChange = (open: boolean) => {
    setSheetOpen(open);
    if (!open) setSelectedNewsId(null);
  };

  const handleNewNewsRefresh = useCallback(() => {
    // Reset infinite query cache and refetch from page 0
    resetAndRefetch();
    // Update lastFetchTime so new-news count resets to 0
    setLastFetchTime(new Date().toISOString());
  }, [resetAndRefetch]);

  const handleFetchNextPage = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  /* Determine active articles list */
  const isAllTab = newsTab === 'all';
  const articles = isAllTab ? allArticles : watchlistArticles;
  const isLoading = isAllTab ? isInfiniteLoading : isWatchlistLoading;

  /* Pick hero article by weighted score:
     abs(sentimentScore - 0.5) * 2  →  strong sentiment (0~1)
     taggedStocks count             →  market-wide impact
     recency (hours ago)            →  fresher is better  */
  const heroArticle = useMemo(() => {
    if (!isAllTab || articles.length === 0) return null;
    const now = Date.now();
    let best = articles[0];
    let bestScore = -1;
    for (const a of articles.slice(0, 20)) {
      const sentimentStrength = Math.abs((a.sentimentScore ?? 0.5) - 0.5) * 2;
      const tagCount = a.taggedStocks?.length ?? 0;
      const hoursAgo = Math.max(1, (now - new Date(a.publishedAt).getTime()) / 3_600_000);
      const score = sentimentStrength * 3 + Math.min(tagCount, 5) * 0.5 + (1 / hoursAgo) * 2
        + (a.isBreaking ? 10 : 0);
      if (score > bestScore) {
        bestScore = score;
        best = a;
      }
    }
    return best;
  }, [isAllTab, articles]);

  const restArticles = useMemo(() => {
    if (!isAllTab) return articles;
    return articles.filter((a) => a !== heroArticle);
  }, [isAllTab, articles, heroArticle]);

  return (
    <div className="mx-auto max-w-screen-xl p-4 pb-8 md:p-6 md:pb-10 space-y-5">

      {/* Page header */}
      <div className="flex items-center gap-2">
        <Newspaper className="h-5 w-5 text-[#3182F6]" />
        <h1 className="text-2xl font-bold text-gray-900">금융 뉴스</h1>
      </div>

      {/* Feature 2: News tab - "전체" | "내 종목" */}
      <div className="flex gap-1 rounded-xl bg-gray-100 p-1">
        <button
          onClick={() => setNewsTab('all')}
          className={cn(
            'flex-1 rounded-lg py-2 text-[13px] font-semibold transition-all',
            newsTab === 'all'
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-500 hover:text-gray-700'
          )}
        >
          전체
        </button>
        <button
          onClick={() => setNewsTab('watchlist')}
          className={cn(
            'flex-1 rounded-lg py-2 text-[13px] font-semibold transition-all flex items-center justify-center gap-1.5',
            newsTab === 'watchlist'
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-500 hover:text-gray-700'
          )}
        >
          <Bell className="h-3.5 w-3.5" />
          내 종목
        </button>
      </div>

      {/* Sentiment filter pills (only for "all" tab) */}
      {isAllTab && (
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
              onClick={() => setSentiment(filter.value)}
            >
              {filter.label}
            </button>
          ))}
        </div>
      )}

      {/* Feature 3: New news banner */}
      {isAllTab && newCount > 0 && (
        <NewNewsBanner count={newCount} onRefresh={handleNewNewsRefresh} />
      )}

      {/* Feature 2: Watchlist tab - login prompt */}
      {newsTab === 'watchlist' && !isAuthenticated && (
        <Card className="rounded-2xl border-0 bg-white shadow-none">
          <CardContent className="flex flex-col items-center gap-3 p-8">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-blue-50">
              <LogIn className="h-6 w-6 text-[#3182F6]" />
            </div>
            <p className="text-[15px] font-semibold text-gray-700">
              로그인하면 관심 종목 뉴스를 모아볼 수 있어요
            </p>
            <p className="text-[13px] text-gray-400">
              관심 종목을 등록하고 맞춤 뉴스를 받아보세요
            </p>
            <Link href="/login">
              <Button className="mt-2 rounded-xl bg-[#3182F6] px-6 py-2.5 text-sm font-semibold text-white hover:bg-[#1B6BF3]">
                로그인
              </Button>
            </Link>
          </CardContent>
        </Card>
      )}

      {/* Watchlist empty state */}
      {newsTab === 'watchlist' && isAuthenticated && watchlist.length === 0 && (
        <Card className="rounded-2xl border-0 bg-white shadow-none">
          <CardContent className="flex flex-col items-center gap-3 p-8">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100">
              <Bell className="h-6 w-6 text-gray-300" />
            </div>
            <p className="text-[15px] font-semibold text-gray-700">
              관심 종목이 없습니다
            </p>
            <p className="text-[13px] text-gray-400">
              종목 상세 페이지에서 하트를 눌러 관심 종목을 추가하세요
            </p>
          </CardContent>
        </Card>
      )}

      {/* News list */}
      {(isAllTab || (newsTab === 'watchlist' && isAuthenticated && watchlist.length > 0)) && (
        <>
          {isLoading ? (
            <NewsListSkeleton count={6} />
          ) : articles.length > 0 ? (
            <div className="space-y-3">

              {/* Hero article (all tab only) */}
              {heroArticle && (
                <button
                  type="button"
                  onClick={() => handleArticleClick(heroArticle.id)}
                  className="w-full text-left"
                >
                  <Card className={cn(
                    'rounded-2xl border-0 shadow-none overflow-hidden transition-all duration-300 hover:shadow-[0_4px_16px_rgba(0,0,0,0.06)] hover:scale-[1.005] relative',
                    getSentimentHeroBg(heroArticle.sentiment)
                  )}>
                    <div className="absolute -left-8 -bottom-8 h-32 w-32 rounded-full bg-blue-100/30 blur-2xl pointer-events-none" />
                    <CardContent className="p-5 md:p-6 relative z-10">
                      <div className="flex items-center gap-2 mb-3">
                        {heroArticle.isBreaking && (
                          <span className="rounded-full bg-red-500 px-2.5 py-0.5 text-[10px] font-bold text-white animate-pulse">
                            속보
                          </span>
                        )}
                        <span className="rounded-full bg-[#3182F6] px-2.5 py-0.5 text-[10px] font-semibold text-white">
                          주요 뉴스
                        </span>
                        <SentimentBadge sentiment={heroArticle.sentiment} />
                      </div>
                      <h2 className="text-lg md:text-xl font-bold text-gray-900 leading-snug line-clamp-2">
                        <TermHighlighter text={heroArticle.title} />
                      </h2>
                      <div className="mt-3 flex items-center gap-2 flex-wrap">
                        <span className="text-xs text-gray-500">
                          {heroArticle.sourceName}
                        </span>
                        <span className="text-[12px] font-medium text-gray-500">
                          {formatRelativeTime(heroArticle.publishedAt)}
                        </span>
                      </div>
                      <StockTagChips article={heroArticle} />
                      <div className="mt-3 flex items-center gap-1.5 text-xs text-[#3182F6] font-semibold">
                        <Sparkles className="h-3.5 w-3.5" />
                        AI 분석 보기
                      </div>
                    </CardContent>
                  </Card>
                </button>
              )}

              {/* Rest articles */}
              {restArticles.length > 0 && (
                <Card className="rounded-2xl border-0 bg-white shadow-none overflow-hidden">
                  <CardContent className="divide-y divide-gray-50 p-0">
                    {restArticles.map((article) => (
                      <button
                        key={article.id}
                        type="button"
                        className="group flex w-full items-stretch text-left transition-all hover:bg-gray-50/50 hover:scale-[1.002]"
                        onClick={() => handleArticleClick(article.id)}
                      >
                        <div className={cn(
                          'w-1 group-hover:w-1.5 shrink-0 transition-all duration-200',
                          getSentimentAccentColor(article.sentiment)
                        )} />
                        <div className="flex flex-1 items-start gap-3 px-4 py-4">
                          <div className="flex-1 min-w-0">
                            {article.isBreaking && (
                              <span className="mr-1.5 inline-block rounded bg-red-500 px-1.5 py-0.5 text-[10px] font-bold text-white leading-none align-middle">
                                속보
                              </span>
                            )}
                            <p className="inline text-sm font-semibold text-gray-900 leading-snug line-clamp-2">
                              <TermHighlighter text={article.title} />
                            </p>
                            <div className="mt-2 flex items-center gap-2 flex-wrap">
                              <SentimentBadge sentiment={article.sentiment} />
                              <span className="text-[11px] text-gray-500">
                                {article.sourceName}
                              </span>
                              <span className="text-[12px] font-medium text-gray-500">
                                {formatRelativeTime(article.publishedAt)}
                              </span>
                            </div>
                            <StockTagChips article={article} />
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

          {/* Feature 10: Infinite scroll (all tab only) */}
          {isAllTab && (
            <InfiniteScrollSentinel
              onIntersect={handleFetchNextPage}
              hasMore={!!hasNextPage}
              isFetchingNextPage={isFetchingNextPage}
            />
          )}

          {/* Watchlist tab: simple pagination */}
          {newsTab === 'watchlist' && watchlistTotalPages > 1 && (
            <div className="flex items-center justify-center gap-3">
              <button
                disabled={watchlistPage <= 0}
                onClick={() => setWatchlistPage((p) => p - 1)}
                className={cn(
                  'rounded-full px-4 py-2 text-[13px] font-semibold transition-colors',
                  watchlistPage <= 0
                    ? 'bg-gray-50 text-gray-300 cursor-not-allowed'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                )}
              >
                이전
              </button>
              <span className="text-sm text-gray-500 tabular-nums">
                {watchlistPage + 1} / {watchlistTotalPages}
              </span>
              <button
                disabled={watchlistPage >= watchlistTotalPages - 1}
                onClick={() => setWatchlistPage((p) => p + 1)}
                className={cn(
                  'rounded-full px-4 py-2 text-[13px] font-semibold transition-colors',
                  watchlistPage >= watchlistTotalPages - 1
                    ? 'bg-gray-50 text-gray-300 cursor-not-allowed'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                )}
              >
                다음
              </button>
            </div>
          )}
        </>
      )}

      {/* Feature 9: Alert keyword management + matched news */}
      {isAuthenticated && (
        <>
          <AlertKeywords />
          {keywordNews && keywordNews.length > 0 && (
            <Card className="rounded-2xl border-0 bg-white shadow-none overflow-hidden">
              <CardContent className="p-0">
                <div className="flex items-center gap-2 px-4 pt-4 pb-2">
                  <Bell className="h-4 w-4 text-[#3182F6]" />
                  <h3 className="text-[14px] font-bold text-gray-900">
                    키워드 매칭 뉴스
                  </h3>
                  <span className="text-[11px] text-gray-400">{keywordNews.length}건</span>
                </div>
                <div className="divide-y divide-gray-50">
                  {keywordNews.slice(0, 10).map((article) => (
                    <button
                      key={article.id}
                      type="button"
                      className="group flex w-full items-stretch text-left transition-all hover:bg-gray-50/50"
                      onClick={() => handleArticleClick(article.id)}
                    >
                      <div className={cn(
                        'w-1 shrink-0',
                        getSentimentAccentColor(article.sentiment)
                      )} />
                      <div className="flex flex-1 items-start gap-3 px-4 py-3">
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-semibold text-gray-900 leading-snug line-clamp-2">
                            <TermHighlighter text={article.title} />
                          </p>
                          <div className="mt-1.5 flex items-center gap-2 flex-wrap">
                            <SentimentBadge sentiment={article.sentiment} />
                            <span className="text-[11px] text-gray-500">{article.sourceName}</span>
                            <span className="text-[11px] text-gray-400">{formatRelativeTime(article.publishedAt)}</span>
                          </div>
                        </div>
                        <Sparkles className="mt-1 h-4 w-4 shrink-0 text-gray-200 group-hover:text-[#3182F6]" />
                      </div>
                    </button>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </>
      )}

      {/* Analysis Sheet */}
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
