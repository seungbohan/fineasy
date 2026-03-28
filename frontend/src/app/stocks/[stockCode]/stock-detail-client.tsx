'use client';

import { use, useState, useMemo } from 'react';
import Link from 'next/link';
import {
  ArrowLeft,
  Heart,
  ExternalLink,
  Sparkles,
  TrendingUp,
  Tag,
  AlertCircle,
  Globe,
  Newspaper,
  BarChart3,
  FileText,
  Calendar,
  Clock,
  Megaphone,
  ChevronLeft,
  ChevronRight,
  MessageSquare,
  ShieldAlert,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
} from '@/components/ui/sheet';
import { SentimentBadge } from '@/components/shared/sentiment-badge';
import { PostWriteForm } from '@/components/community/post-write-form';
import { PostList } from '@/components/community/post-list';
import { StockChart } from '@/components/stocks/stock-chart';
import { CompanyAnalysisCard } from '@/components/stocks/company-analysis-card';
import { FundamentalAnalysis } from '@/components/stocks/fundamental-analysis';
import { KeyNewsCard } from '@/components/stocks/key-news-card';
import { useStockDetail } from '@/hooks/use-stocks';
import { useStockNews, useNewsAnalysis, useStockNewsSummary, useSentimentTrend } from '@/hooks/use-news';
import { useAnalysisReport } from '@/hooks/use-analysis';
import { useDomesticDisclosure, useOverseasDisclosure, useDisclosureSummary } from '@/hooks/use-disclosure';
import { SentimentTrendChart } from '@/components/shared/sentiment-trend-chart';
import { useRouter } from 'next/navigation';
import { useWatchlistStore } from '@/stores/watchlist-store';
import { useAuthStore } from '@/stores/auth-store';
import {
  formatPrice,
  formatChange,
  formatChangeRate,
  formatRelativeTime,
  getPriceColorClass,
  getPriceArrow,
  getCurrencyFromMarket,
} from '@/lib/format';
import type { DomesticDisclosure, OverseasDisclosure } from '@/types';

/** Returns true if stockCode is a domestic Korean stock (6-digit number) */
function isDomesticStock(stockCode: string): boolean {
  return /^\d{6}$/.test(stockCode);
}

/** Format date string to localized short format */
function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

/** Badge color mapping for disclosure types */
function getDisclosureTypeBadgeClass(type: string): string {
  const lower = type.toLowerCase();
  if (lower.includes('정기') || lower === '10-k' || lower === '10-q') {
    return 'bg-blue-50 text-[#3182F6]';
  }
  if (lower.includes('주요') || lower === '8-k') {
    return 'bg-red-50 text-red-500';
  }
  if (lower.includes('자본') || lower.includes('배당')) {
    return 'bg-emerald-50 text-emerald-600';
  }
  if (lower.includes('지분') || lower.includes('소유')) {
    return 'bg-amber-50 text-amber-600';
  }
  if (lower.includes('실적')) {
    return 'bg-purple-50 text-purple-600';
  }
  if (lower.includes('정정')) {
    return 'bg-orange-50 text-orange-500';
  }
  if (lower.includes('감사')) {
    return 'bg-slate-100 text-slate-600';
  }
  return 'bg-gray-100 text-gray-600';
}

/** Format disclosure date with relative time (e.g. "2025.03.20 (5일 전)") */
function formatDisclosureDate(dateStr: string): string {
  // DART dates come as "yyyyMMdd" format
  const formatted = dateStr.length === 8
    ? `${dateStr.slice(0, 4)}.${dateStr.slice(4, 6)}.${dateStr.slice(6, 8)}`
    : dateStr;

  const year = dateStr.length === 8 ? parseInt(dateStr.slice(0, 4)) : NaN;
  const month = dateStr.length === 8 ? parseInt(dateStr.slice(4, 6)) - 1 : NaN;
  const day = dateStr.length === 8 ? parseInt(dateStr.slice(6, 8)) : NaN;

  if (isNaN(year)) return formatted;

  const date = new Date(year, month, day);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) return `${formatted} (오늘)`;
  if (diffDays === 1) return `${formatted} (어제)`;
  if (diffDays <= 30) return `${formatted} (${diffDays}일 전)`;
  if (diffDays <= 365) {
    const months = Math.floor(diffDays / 30);
    return `${formatted} (${months}개월 전)`;
  }
  return formatted;
}

export default function StockDetailPage({
  params,
}: {
  params: Promise<{ stockCode: string }>;
}) {
  const { stockCode } = use(params);
  const { data: stock, isLoading } = useStockDetail(stockCode);
  const { data: news } = useStockNews(stockCode);
  const { data: report } = useAnalysisReport(stockCode);
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const { isWatched, addStock, removeStock } = useWatchlistStore();

  const [selectedNewsId, setSelectedNewsId] = useState<number | null>(null);
  const [sheetOpen, setSheetOpen] = useState(false);
  const { data: analysisData, isLoading: isAnalysisLoading, isError: isAnalysisError } = useNewsAnalysis(selectedNewsId);

  /** Active tab: 'info' (default), 'disclosure', 'timeline', or 'community' */
  const [activeTab, setActiveTab] = useState<'info' | 'disclosure' | 'timeline' | 'community'>('info');

  const watched = isWatched(stockCode);
  const isDomestic = isDomesticStock(stockCode);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-screen-xl p-4 space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-12 w-32" />
        <Skeleton className="h-[280px] w-full rounded-lg" />
      </div>
    );
  }

  if (!stock) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <p className="text-lg font-medium text-gray-900">
          종목을 찾을 수 없습니다
        </p>
        <Link href="/">
          <Button variant="outline">홈으로 돌아가기</Button>
        </Link>
      </div>
    );
  }

  const colorClass = getPriceColorClass(stock.changeAmount);
  const currency = stock.currency ?? getCurrencyFromMarket(stock.market);
  const isUsd = currency === 'USD';

  return (
    <div className="mx-auto max-w-screen-xl">
      {/* Sticky header */}
      <div className="sticky top-16 z-30 bg-white/80 backdrop-blur-xl shadow-[0_1px_3px_rgba(0,0,0,0.04)] px-4 py-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Link
              href="/"
              className="text-gray-400 hover:text-gray-600 transition-colors"
              aria-label="뒤로가기"
            >
              <ArrowLeft className="h-5 w-5" />
            </Link>
            <div>
              <h1 className="text-base font-bold text-gray-900">
                {stock.stockName}
              </h1>
              <p className="text-xs text-gray-500">
                {stock.stockCode} &middot; {stock.market}
              </p>
            </div>
          </div>
          <button
            onClick={() => {
              if (!isAuthenticated) {
                if (confirm('로그인이 필요한 기능입니다.\n로그인 페이지로 이동하시겠습니까?')) {
                  router.push('/login');
                }
                return;
              }
              watched ? removeStock(stockCode) : addStock(stockCode);
            }}
            className={`transition-colors ${
              watched
                ? 'text-red-500 hover:text-red-600'
                : 'text-gray-300 hover:text-gray-400'
            }`}
            aria-label={watched ? '관심 종목 해제' : '관심 종목 추가'}
          >
            <Heart className={`h-6 w-6 ${watched ? 'fill-current' : ''}`} />
          </button>
        </div>
      </div>

      {/* Price section - always visible */}
      <div className="px-4 pt-4 pb-2">
        <div className="px-4 py-3 rounded-2xl bg-gradient-to-br from-gray-50/80 to-white">
          <p className="text-3xl font-bold tabular-nums text-gray-900">
            {isUsd
              ? formatPrice(stock.currentPrice, 'USD')
              : (
                <>
                  {formatPrice(stock.currentPrice)}
                  <span className="text-lg text-gray-400 ml-0.5">원</span>
                </>
              )
            }
          </p>
          <p className={`mt-1 text-base font-semibold tabular-nums ${colorClass}`}>
            {getPriceArrow(stock.changeAmount)}{' '}
            {isUsd
              ? formatChange(stock.changeAmount, 'USD')
              : <>{formatChange(stock.changeAmount)}원</>
            } (
            {formatChangeRate(stock.changeRate)})
          </p>
        </div>
      </div>

      {/* Tab navigation */}
      <div className="px-4">
        <div className="flex gap-1 rounded-xl bg-gray-100 p-1">
          <button
            onClick={() => setActiveTab('info')}
            className={`flex-1 rounded-lg py-2 text-[13px] font-semibold transition-all ${
              activeTab === 'info'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            종목정보
          </button>
          <button
            onClick={() => setActiveTab('disclosure')}
            className={`flex-1 rounded-lg py-2 text-[13px] font-semibold transition-all flex items-center justify-center gap-1.5 ${
              activeTab === 'disclosure'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            <FileText className="h-3.5 w-3.5" />
            공시
          </button>
          <button
            onClick={() => setActiveTab('timeline')}
            className={`flex-1 rounded-lg py-2 text-[13px] font-semibold transition-all flex items-center justify-center gap-1.5 ${
              activeTab === 'timeline'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            <Clock className="h-3.5 w-3.5" />
            타임라인
          </button>
          <button
            onClick={() => setActiveTab('community')}
            className={`flex-1 rounded-lg py-2 text-[13px] font-semibold transition-all flex items-center justify-center gap-1.5 ${
              activeTab === 'community'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            <MessageSquare className="h-3.5 w-3.5" />
            토론
          </button>
        </div>
      </div>

      {/* Tab content */}
      {activeTab === 'info' ? (
        <InfoTab
          stockCode={stockCode}
          stock={stock}
          news={news ?? null}
          report={report ?? null}
          isUsd={isUsd}
          selectedNewsId={selectedNewsId}
          setSelectedNewsId={setSelectedNewsId}
          sheetOpen={sheetOpen}
          setSheetOpen={setSheetOpen}
          analysisData={analysisData ?? null}
          isAnalysisLoading={isAnalysisLoading}
          isAnalysisError={isAnalysisError}
        />
      ) : activeTab === 'disclosure' ? (
        <DisclosureTab stockCode={stockCode} isDomestic={isDomestic} />
      ) : activeTab === 'community' ? (
        <CommunityTab stockCode={stockCode} />
      ) : (
        <TimelineTab stockCode={stockCode} isDomestic={isDomestic} />
      )}

      {/* News Analysis Sheet */}
      <Sheet open={sheetOpen} onOpenChange={(open) => {
        setSheetOpen(open);
        if (!open) setSelectedNewsId(null);
      }}>
        <SheetContent
          side="bottom"
          className="mx-auto max-w-screen-md rounded-t-3xl max-h-[85vh] overflow-y-auto"
        >
          <div className="flex justify-center pt-3 pb-1">
            <div className="h-1.5 w-12 rounded-full bg-gray-200" />
          </div>

          <SheetHeader className="px-4 pb-0">
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
            <div className="space-y-5 px-4 py-2">
              <div className="space-y-2"><Skeleton className="h-4 w-20" /><Skeleton className="h-4 w-full" /><Skeleton className="h-4 w-3/4" /></div>
              <div className="space-y-2"><Skeleton className="h-4 w-24" /><Skeleton className="h-4 w-full" /><Skeleton className="h-4 w-5/6" /></div>
              <div className="space-y-2"><Skeleton className="h-4 w-20" /><div className="flex gap-2"><Skeleton className="h-7 w-16 rounded-full" /><Skeleton className="h-7 w-20 rounded-full" /></div></div>
              <div className="space-y-2"><Skeleton className="h-4 w-24" /><Skeleton className="h-4 w-full" /></div>
            </div>
          ) : isAnalysisError ? (
            <div className="flex flex-col items-center justify-center gap-3 px-4 py-12">
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100">
                <AlertCircle className="h-6 w-6 text-gray-400" />
              </div>
              <p className="text-sm text-gray-500">분석을 불러올 수 없습니다</p>
              <p className="text-xs text-gray-400">잠시 후 다시 시도해 주세요</p>
            </div>
          ) : analysisData ? (
            <div className="space-y-5 px-4 py-2">
              <section>
                <div className="flex items-center gap-1.5 mb-2">
                  <Sparkles className="h-3.5 w-3.5 text-[#3182F6]" />
                  <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide">AI 요약</h3>
                </div>
                <p className="text-sm text-gray-800 leading-relaxed bg-blue-50/50 rounded-xl p-3">
                  {analysisData.analysis.summary}
                </p>
              </section>

              <section>
                <div className="flex items-center gap-1.5 mb-2">
                  <TrendingUp className="h-3.5 w-3.5 text-[#3182F6]" />
                  <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide">시장 영향</h3>
                </div>
                <p className="text-sm text-gray-700 leading-relaxed">
                  {analysisData.analysis.marketImpact}
                </p>
              </section>

              {analysisData.analysis.relatedStocks?.length > 0 && (
                <section>
                  <div className="flex items-center gap-1.5 mb-2">
                    <Tag className="h-3.5 w-3.5 text-[#3182F6]" />
                    <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide">관련 종목</h3>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {analysisData.analysis.relatedStocks.map((s) => (
                      <span key={s} className="inline-flex items-center rounded-full bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-700">
                        {s}
                      </span>
                    ))}
                  </div>
                </section>
              )}
            </div>
          ) : null}

          {analysisData && !isAnalysisLoading && !isAnalysisError && (
            <SheetFooter className="px-4 pb-6">
              <a
                href={analysisData.originalUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-gray-100 px-4 py-3 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-200"
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

/* ────────────────────────────────────────────────────────
 * InfoTab - existing stock detail content (chart, analysis, news)
 * ──────────────────────────────────────────────────────── */

function InfoTab({
  stockCode,
  stock,
  news,
  report,
  isUsd,
  selectedNewsId,
  setSelectedNewsId,
  sheetOpen,
  setSheetOpen,
  analysisData,
  isAnalysisLoading,
  isAnalysisError,
}: {
  stockCode: string;
  stock: NonNullable<ReturnType<typeof useStockDetail>['data']>;
  news: import('@/types').NewsArticle[] | null;
  report: import('@/types').AnalysisReport | null;
  isUsd: boolean;
  selectedNewsId: number | null;
  setSelectedNewsId: (id: number | null) => void;
  sheetOpen: boolean;
  setSheetOpen: (open: boolean) => void;
  analysisData: import('@/types').NewsAnalysisResponse | null;
  isAnalysisLoading: boolean;
  isAnalysisError: boolean;
}) {
  return (
    <div className="space-y-2 p-4">
      <div className="rounded-xl bg-white overflow-hidden">
        <StockChart stockCode={stockCode} market={stock.market} />
      </div>

      <CompanyAnalysisCard stockCode={stockCode} stock={stock} />

      <FundamentalAnalysis stockCode={stockCode} market={stock.market} />

      <KeyNewsCard
        stockCode={stockCode}
        onNewsClick={(newsId) => {
          setSelectedNewsId(newsId);
          setSheetOpen(true);
        }}
      />

      {report?.technicalSignals && (
        <Card className="rounded-xl border-0 bg-white shadow-none">
          <CardContent className="p-4">
            <div className="mb-3 flex items-center gap-2">
              <Newspaper className="h-4.5 w-4.5 text-[#3182F6]" />
              <h2 className="text-[15px] font-bold text-gray-900">
                뉴스 기반 분석
              </h2>
              <span className="rounded-full bg-blue-50 px-2.5 py-0.5 text-[11px] font-semibold text-[#3182F6]">
                AI
              </span>
            </div>

            {report.technicalSignals.newsAnalysis && (
              <div className="mb-3">
                <div className="flex items-center gap-2 mb-1.5">
                  <div className="flex h-6 w-6 items-center justify-center rounded-full bg-blue-50">
                    <Newspaper className="h-3 w-3 text-[#3182F6]" />
                  </div>
                  <span className="text-[13px] font-bold text-gray-700">뉴스 분석</span>
                </div>
                <p className="text-sm text-gray-700 leading-relaxed bg-blue-50/50 rounded-xl p-3">
                  {report.technicalSignals.newsAnalysis}
                </p>
              </div>
            )}

            {report.technicalSignals.macroImpact && (
              <div className="mb-3">
                <div className="flex items-center gap-2 mb-1.5">
                  <div className="flex h-6 w-6 items-center justify-center rounded-full bg-amber-50">
                    <BarChart3 className="h-3 w-3 text-amber-500" />
                  </div>
                  <span className="text-[13px] font-bold text-gray-700">거시경제 영향</span>
                </div>
                <p className="text-sm text-gray-700 leading-relaxed bg-amber-50/50 rounded-xl p-3">
                  {report.technicalSignals.macroImpact}
                </p>
              </div>
            )}

            {report.technicalSignals.globalEventImpact && (
              <div className="mb-3">
                <div className="flex items-center gap-2 mb-1.5">
                  <div className="flex h-6 w-6 items-center justify-center rounded-full bg-emerald-50">
                    <Globe className="h-3 w-3 text-emerald-500" />
                  </div>
                  <span className="text-[13px] font-bold text-gray-700">글로벌 이벤트 영향</span>
                </div>
                <p className="text-sm text-gray-700 leading-relaxed bg-emerald-50/50 rounded-xl p-3">
                  {report.technicalSignals.globalEventImpact}
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Feature 4: AI News Summary Card */}
      <StockNewsSummaryCard stockCode={stockCode} />

      {/* Feature 5: Sentiment Trend Chart */}
      <SentimentTrendSection stockCode={stockCode} />

      {/* Related news section */}
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <h2 className="mb-3 text-[15px] font-bold text-gray-900">
            관련 뉴스
          </h2>
          {news && news.length > 0 ? (
            <div className="divide-y divide-gray-100">
              {news.map((article) => (
                <button
                  key={article.id}
                  type="button"
                  className="group flex w-full items-start gap-2 py-3 text-left transition-colors hover:bg-gray-50 -mx-1 px-1 rounded-lg"
                  onClick={() => {
                    setSelectedNewsId(article.id);
                    setSheetOpen(true);
                  }}
                >
                  <span className={`mt-2 h-2 w-2 shrink-0 rounded-full ${
                    article.sentiment === 'POSITIVE' ? 'bg-red-400' :
                    article.sentiment === 'NEGATIVE' ? 'bg-blue-400' :
                    'bg-gray-300'
                  }`} />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 line-clamp-2 leading-snug">
                      {article.title}
                    </p>
                    <div className="mt-1.5 flex items-center gap-2">
                      <SentimentBadge sentiment={article.sentiment} />
                      <span className="text-[11px] text-gray-500">
                        {article.sourceName}
                      </span>
                      <span className="text-[11px] text-gray-400">
                        {formatRelativeTime(article.publishedAt)}
                      </span>
                    </div>
                  </div>
                  <Sparkles className="mt-1 h-3.5 w-3.5 shrink-0 text-gray-300 transition-colors group-hover:text-[#3182F6]" />
                </button>
              ))}
            </div>
          ) : (
            <p className="py-4 text-center text-sm text-gray-400">
              관련 뉴스가 없습니다
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

/* ────────────────────────────────────────────────────────
 * DisclosureTab - DART (domestic) or SEC EDGAR (overseas)
 * ──────────────────────────────────────────────────────── */

function DisclosureTab({
  stockCode,
  isDomestic,
}: {
  stockCode: string;
  isDomestic: boolean;
}) {
  const [selectedReceiptNumber, setSelectedReceiptNumber] = useState<string | null>(null);
  const [summarySheetOpen, setSummarySheetOpen] = useState(false);

  const handleOpenSummary = (receiptNumber: string) => {
    setSelectedReceiptNumber(receiptNumber);
    setSummarySheetOpen(true);
  };

  return (
    <>
      {isDomestic ? (
        <DomesticDisclosureList stockCode={stockCode} onOpenSummary={handleOpenSummary} />
      ) : (
        <OverseasDisclosureList stockCode={stockCode} />
      )}

      {/* Disclosure Summary Sheet */}
      <DisclosureSummarySheet
        stockCode={stockCode}
        receiptNumber={selectedReceiptNumber}
        open={summarySheetOpen}
        onOpenChange={(open) => {
          setSummarySheetOpen(open);
          if (!open) setSelectedReceiptNumber(null);
        }}
      />
    </>
  );
}

/** Skeleton for disclosure list loading state */
function DisclosureSkeleton() {
  return (
    <div className="space-y-3 p-4">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className="rounded-xl bg-white p-4 space-y-2">
          <div className="flex items-center gap-2">
            <Skeleton className="h-5 w-14 rounded-full" />
            <Skeleton className="h-4 w-20" />
          </div>
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-2/3" />
        </div>
      ))}
    </div>
  );
}

/** Domestic disclosure list (DART) */
function DomesticDisclosureList({
  stockCode,
  onOpenSummary,
}: {
  stockCode: string;
  onOpenSummary: (receiptNumber: string) => void;
}) {
  const { data, isLoading, isError } = useDomesticDisclosure(stockCode);

  if (isLoading) return <DisclosureSkeleton />;

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 px-4 py-16">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100">
          <AlertCircle className="h-6 w-6 text-gray-400" />
        </div>
        <p className="text-sm text-gray-500">공시 정보를 불러올 수 없습니다</p>
        <p className="text-xs text-gray-400">잠시 후 다시 시도해 주세요</p>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 px-4 py-16">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100">
          <FileText className="h-6 w-6 text-gray-300" />
        </div>
        <p className="text-sm text-gray-500">공시 내역이 없습니다</p>
      </div>
    );
  }

  return (
    <div className="p-4 space-y-2">
      <div className="flex items-center justify-between mb-1">
        <p className="text-[13px] text-gray-500">
          최근 공시 <span className="font-semibold text-gray-700">{data.length}</span>건
        </p>
        <span className="text-[11px] text-gray-400">DART</span>
      </div>

      <div className="space-y-2">
        {data.map((item) => (
          <button
            key={item.id}
            onClick={() => onOpenSummary(item.receiptNumber)}
            className="group block w-full text-left rounded-xl bg-white p-4 transition-colors hover:bg-gray-50 active:bg-gray-100"
          >
            <div className="flex items-center gap-2 mb-1.5">
              <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold ${getDisclosureTypeBadgeClass(item.disclosureType)}`}>
                {item.disclosureType}
              </span>
            </div>
            <p className="text-[13px] font-medium text-gray-900 leading-snug line-clamp-2 group-hover:text-[#3182F6] transition-colors">
              {item.title}
            </p>
            <div className="mt-1.5 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-[11px] text-gray-400">{item.submitter}</span>
                <span className="flex items-center gap-1 text-[11px] text-gray-400">
                  <Calendar className="h-3 w-3" />
                  {formatDisclosureDate(item.filingDate)}
                </span>
              </div>
              <Sparkles className="h-3.5 w-3.5 text-gray-300 group-hover:text-[#3182F6] transition-colors" />
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}

/** Disclosure Summary Sheet - AI-generated summary for DART disclosures */
function DisclosureSummarySheet({
  stockCode,
  receiptNumber,
  open,
  onOpenChange,
}: {
  stockCode: string;
  receiptNumber: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const { data, isLoading, isError } = useDisclosureSummary(
    stockCode,
    open ? receiptNumber : null
  );

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="bottom"
        className="mx-auto max-w-screen-md rounded-t-3xl max-h-[85vh] overflow-y-auto"
      >
        <div className="flex justify-center pt-3 pb-1">
          <div className="h-1.5 w-12 rounded-full bg-gray-200" />
        </div>

        <SheetHeader className="px-4 pb-0">
          <SheetTitle className="text-base font-bold text-gray-900 leading-snug pr-6">
            {data?.reportName ?? '공시 AI 요약'}
          </SheetTitle>
          <SheetDescription asChild>
            <div className="flex items-center gap-2 flex-wrap pt-1">
              {data && (
                <>
                  <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold ${getDisclosureTypeBadgeClass(data.disclosureType)}`}>
                    {data.disclosureType}
                  </span>
                  <span className="text-[11px] text-gray-400">
                    {data.filerName}
                  </span>
                  <span className="text-[11px] text-gray-400">
                    {formatDate(data.filingDate)}
                  </span>
                </>
              )}
            </div>
          </SheetDescription>
        </SheetHeader>

        {isLoading ? (
          <div className="space-y-5 px-4 py-2">
            <div className="space-y-2"><Skeleton className="h-4 w-20" /><Skeleton className="h-4 w-full" /><Skeleton className="h-4 w-3/4" /></div>
            <div className="space-y-2"><Skeleton className="h-4 w-24" /><Skeleton className="h-4 w-full" /><Skeleton className="h-4 w-5/6" /></div>
            <div className="space-y-2"><Skeleton className="h-4 w-20" /><div className="flex gap-2"><Skeleton className="h-7 w-16 rounded-full" /><Skeleton className="h-7 w-20 rounded-full" /></div></div>
          </div>
        ) : isError ? (
          <div className="flex flex-col items-center justify-center gap-3 px-4 py-12">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100">
              <AlertCircle className="h-6 w-6 text-gray-400" />
            </div>
            <p className="text-sm text-gray-500">요약을 불러올 수 없습니다</p>
            <p className="text-xs text-gray-400">잠시 후 다시 시도해 주세요</p>
          </div>
        ) : data ? (
          <div className="space-y-5 px-4 py-2">
            <section>
              <div className="flex items-center gap-1.5 mb-2">
                <Sparkles className="h-3.5 w-3.5 text-[#3182F6]" />
                <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide">AI 요약</h3>
              </div>
              <p className="text-sm text-gray-800 leading-relaxed bg-blue-50/50 rounded-xl p-3">
                {data.summary.overview}
              </p>
            </section>

            <section>
              <div className="flex items-center gap-1.5 mb-2">
                <FileText className="h-3.5 w-3.5 text-[#3182F6]" />
                <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide">핵심 내용</h3>
              </div>
              <p className="text-sm text-gray-700 leading-relaxed">
                {data.summary.keyPoints}
              </p>
            </section>

            {data.summary.highlights?.length > 0 && (
              <section>
                <div className="flex items-center gap-1.5 mb-2">
                  <Megaphone className="h-3.5 w-3.5 text-[#3182F6]" />
                  <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide">주요 포인트</h3>
                </div>
                <ul className="space-y-1.5">
                  {data.summary.highlights.map((h, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm text-gray-700">
                      <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-[#3182F6]" />
                      {h}
                    </li>
                  ))}
                </ul>
              </section>
            )}

            {data.summary.investorImplication && (
              <section>
                <div className="flex items-center gap-1.5 mb-2">
                  <TrendingUp className="h-3.5 w-3.5 text-[#3182F6]" />
                  <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide">투자자 시사점</h3>
                </div>
                <p className="text-sm text-gray-700 leading-relaxed">
                  {data.summary.investorImplication}
                </p>
              </section>
            )}

            <p className="text-[11px] text-gray-400 leading-relaxed">
              {data.summary.disclaimer}
            </p>
          </div>
        ) : null}

        {data && !isLoading && !isError && (
          <SheetFooter className="px-4 pb-6">
            <a
              href={data.dartUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-gray-100 px-4 py-3 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-200"
            >
              <ExternalLink className="h-4 w-4" />
              DART 원문 보기
            </a>
          </SheetFooter>
        )}
      </SheetContent>
    </Sheet>
  );
}

/* ────────────────────────────────────────────────────────
 * StockNewsSummaryCard - AI-generated stock news summary
 * ──────────────────────────────────────────────────────── */

function StockNewsSummaryCard({ stockCode }: { stockCode: string }) {
  const { data, isLoading } = useStockNewsSummary(stockCode);

  if (isLoading) {
    return (
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4 space-y-3">
          <div className="flex items-center gap-2">
            <Skeleton className="h-5 w-5 rounded" />
            <Skeleton className="h-5 w-32" />
          </div>
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-4/5" />
          <Skeleton className="h-4 w-3/5" />
        </CardContent>
      </Card>
    );
  }

  if (!data) return null;

  return (
    <Card className="rounded-xl border-0 bg-gradient-to-br from-blue-50/60 to-white shadow-none overflow-hidden relative">
      <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-blue-100/30 blur-2xl pointer-events-none" />
      <CardContent className="p-4 relative z-10">
        <div className="mb-3 flex items-center gap-2">
          <Sparkles className="h-4 w-4 text-[#3182F6]" />
          <h2 className="text-[15px] font-bold text-gray-900">
            오늘의 뉴스 요약
          </h2>
          <span className="rounded-full bg-blue-50 px-2.5 py-0.5 text-[11px] font-semibold text-[#3182F6]">
            AI
          </span>
        </div>
        <p className="text-sm text-gray-700 leading-relaxed bg-blue-50/50 rounded-xl p-3">
          {data.summary}
        </p>
        <p className="mt-2 text-[11px] text-gray-400">
          {data.articleCount}건의 뉴스 기반 &middot; {formatRelativeTime(data.generatedAt)}
        </p>
      </CardContent>
    </Card>
  );
}

/* ────────────────────────────────────────────────────────
 * SentimentTrendSection - News sentiment trend chart
 * ──────────────────────────────────────────────────────── */

function SentimentTrendSection({ stockCode }: { stockCode: string }) {
  const { data, isLoading } = useSentimentTrend(stockCode);

  if (isLoading) {
    return (
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4 space-y-3">
          <Skeleton className="h-5 w-36" />
          <Skeleton className="h-[180px] w-full rounded-lg" />
        </CardContent>
      </Card>
    );
  }

  if (!data || !data.points || data.points.length < 2) return null;

  return (
    <Card className="rounded-xl border-0 bg-white shadow-none">
      <CardContent className="p-4">
        <div className="mb-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <TrendingUp className="h-4 w-4 text-[#3182F6]" />
            <h2 className="text-[15px] font-bold text-gray-900">
              뉴스 감성 트렌드
            </h2>
          </div>
          <span className="text-[11px] text-gray-400">
            평균{' '}
            <span className={data.averageScore >= 0.5 ? 'text-up font-semibold' : 'text-down font-semibold'}>
              {(data.averageScore * 100).toFixed(0)}점
            </span>
          </span>
        </div>
        <SentimentTrendChart points={data.points} />
      </CardContent>
    </Card>
  );
}

/* ────────────────────────────────────────────────────────
 * TimelineTab - Combined disclosure + news timeline
 * ──────────────────────────────────────────────────────── */

interface TimelineItem {
  id: string;
  type: 'news' | 'disclosure';
  title: string;
  date: string;
  meta?: string;
  url?: string;
  sentiment?: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  disclosureType?: string;
}

function TimelineTab({
  stockCode,
  isDomestic,
}: {
  stockCode: string;
  isDomestic: boolean;
}) {
  const [currentMonth, setCurrentMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [selectedDate, setSelectedDate] = useState<string | null>(null);

  const { data: news, isLoading: isNewsLoading } = useStockNews(stockCode);
  const { data: domesticDisclosures, isLoading: isDomLoading } =
    useDomesticDisclosure(isDomestic ? stockCode : '');
  const { data: overseasDisclosures, isLoading: isOverseasLoading } =
    useOverseasDisclosure(!isDomestic ? stockCode : '');

  const isLoading = isNewsLoading || (isDomestic ? isDomLoading : isOverseasLoading);

  const items: TimelineItem[] = useMemo(() => {
    const result: TimelineItem[] = [];
    (news ?? []).forEach((article) => {
      result.push({
        id: `news-${article.id}`,
        type: 'news',
        title: article.title,
        date: article.publishedAt,
        meta: article.sourceName,
        url: article.originalUrl,
        sentiment: article.sentiment,
      });
    });
    if (isDomestic && domesticDisclosures) {
      domesticDisclosures.forEach((d) => {
        result.push({
          id: `disc-${d.id}`,
          type: 'disclosure',
          title: d.title,
          date: d.filingDate,
          meta: d.submitter,
          url: d.dartUrl,
          disclosureType: d.disclosureType,
        });
      });
    }
    if (!isDomestic && overseasDisclosures) {
      overseasDisclosures.forEach((d) => {
        result.push({
          id: `disc-${d.id}`,
          type: 'disclosure',
          title: d.title,
          date: d.filingDate,
          url: d.edgarUrl,
          disclosureType: d.filingType,
        });
      });
    }
    result.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
    return result;
  }, [news, domesticDisclosures, overseasDisclosures, isDomestic]);

  // Group items by date (YYYY-MM-DD)
  const itemsByDate = useMemo(() => {
    const map = new Map<string, TimelineItem[]>();
    items.forEach((item) => {
      const key = item.date.slice(0, 10);
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(item);
    });
    return map;
  }, [items]);

  // Calendar helpers
  const year = currentMonth.getFullYear();
  const month = currentMonth.getMonth();
  const firstDay = new Date(year, month, 1).getDay(); // 0=Sun
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const today = new Date().toISOString().slice(0, 10);

  const prevMonth = () => setCurrentMonth(new Date(year, month - 1, 1));
  const nextMonth = () => setCurrentMonth(new Date(year, month + 1, 1));

  const selectedItems = selectedDate ? (itemsByDate.get(selectedDate) ?? []) : [];

  if (isLoading) {
    return (
      <div className="p-4 space-y-3">
        <Skeleton className="h-8 w-40 mx-auto" />
        <Skeleton className="h-[260px] w-full rounded-xl" />
      </div>
    );
  }

  return (
    <div className="p-4 space-y-3">
      {/* Calendar header */}
      <div className="flex items-center justify-between px-1">
        <button onClick={prevMonth} className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors">
          <ChevronLeft className="h-4 w-4 text-gray-500" />
        </button>
        <span className="text-[15px] font-bold text-gray-900">
          {year}년 {month + 1}월
        </span>
        <button onClick={nextMonth} className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors">
          <ChevronRight className="h-4 w-4 text-gray-500" />
        </button>
      </div>

      {/* Calendar grid */}
      <div className="rounded-xl bg-white overflow-hidden">
        {/* Day headers */}
        <div className="grid grid-cols-7 text-center">
          {['일', '월', '화', '수', '목', '금', '토'].map((d, i) => (
            <div key={d} className={`py-2 text-[11px] font-semibold ${i === 0 ? 'text-red-400' : i === 6 ? 'text-blue-400' : 'text-gray-400'}`}>
              {d}
            </div>
          ))}
        </div>
        {/* Day cells */}
        <div className="grid grid-cols-7">
          {Array.from({ length: firstDay }).map((_, i) => (
            <div key={`empty-${i}`} className="h-11" />
          ))}
          {Array.from({ length: daysInMonth }).map((_, i) => {
            const day = i + 1;
            const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
            const dayItems = itemsByDate.get(dateStr);
            const hasNews = dayItems?.some((it) => it.type === 'news');
            const hasDisc = dayItems?.some((it) => it.type === 'disclosure');
            const isSelected = selectedDate === dateStr;
            const isToday = dateStr === today;
            const dayOfWeek = new Date(year, month, day).getDay();

            return (
              <button
                key={day}
                onClick={() => setSelectedDate(isSelected ? null : dateStr)}
                className={`relative h-11 flex flex-col items-center justify-center transition-colors ${
                  isSelected
                    ? 'bg-gray-900 text-white rounded-lg'
                    : isToday
                      ? 'bg-blue-50 rounded-lg'
                      : 'hover:bg-gray-50'
                } ${!isSelected && dayOfWeek === 0 ? 'text-red-400' : ''} ${!isSelected && dayOfWeek === 6 ? 'text-blue-400' : ''}`}
              >
                <span className={`text-[13px] tabular-nums ${isSelected ? 'font-bold' : dayItems ? 'font-semibold' : 'font-normal text-gray-500'}`}>
                  {day}
                </span>
                {dayItems && (
                  <div className="flex gap-0.5 mt-0.5">
                    {hasNews && <span className={`h-1 w-1 rounded-full ${isSelected ? 'bg-blue-300' : 'bg-[#3182F6]'}`} />}
                    {hasDisc && <span className={`h-1 w-1 rounded-full ${isSelected ? 'bg-orange-300' : 'bg-orange-400'}`} />}
                  </div>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Legend */}
      <div className="flex items-center justify-center gap-4 py-1">
        <div className="flex items-center gap-1.5">
          <span className="h-2 w-2 rounded-full bg-[#3182F6]" />
          <span className="text-[11px] text-gray-400">뉴스</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-2 w-2 rounded-full bg-orange-400" />
          <span className="text-[11px] text-gray-400">공시</span>
        </div>
      </div>

      {/* Selected date items */}
      {selectedDate && (
        <div className="space-y-2">
          <p className="text-[13px] font-semibold text-gray-700 px-1">
            {new Date(selectedDate).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' })}
            <span className="ml-1.5 text-gray-400 font-normal">{selectedItems.length}건</span>
          </p>
          {selectedItems.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-6">이 날짜에 이벤트가 없습니다</p>
          ) : (
            <div className="space-y-2">
              {selectedItems.map((item) => {
                const isNews = item.type === 'news';
                return (
                  <a
                    key={item.id}
                    href={item.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="group block rounded-xl bg-white p-3 transition-colors hover:bg-gray-50"
                  >
                    <div className="flex items-center gap-2 mb-1">
                      <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${
                        isNews ? 'bg-blue-50 text-[#3182F6]' : 'bg-orange-50 text-orange-600'
                      }`}>
                        {isNews ? '뉴스' : '공시'}
                      </span>
                      {item.disclosureType && (
                        <span className={`text-[10px] font-medium px-1.5 py-0.5 rounded ${getDisclosureTypeBadgeClass(item.disclosureType)}`}>
                          {item.disclosureType}
                        </span>
                      )}
                      {item.sentiment && <SentimentBadge sentiment={item.sentiment} />}
                    </div>
                    <p className="text-[13px] font-medium text-gray-900 leading-snug line-clamp-2 group-hover:text-[#3182F6] transition-colors">
                      {item.title}
                    </p>
                    <div className="mt-1 flex items-center gap-2">
                      <span className="text-[11px] text-gray-400">{formatRelativeTime(item.date)}</span>
                      {item.meta && <span className="text-[11px] text-gray-400">{item.meta}</span>}
                    </div>
                  </a>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* No data at all */}
      {items.length === 0 && (
        <div className="flex flex-col items-center justify-center gap-2 py-8">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100">
            <Calendar className="h-6 w-6 text-gray-300" />
          </div>
          <p className="text-sm text-gray-500">이벤트 데이터가 없습니다</p>
        </div>
      )}
    </div>
  );
}

/** Overseas disclosure list (SEC EDGAR) */
function OverseasDisclosureList({ stockCode }: { stockCode: string }) {
  const { data, isLoading, isError } = useOverseasDisclosure(stockCode);

  if (isLoading) return <DisclosureSkeleton />;

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 px-4 py-16">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100">
          <AlertCircle className="h-6 w-6 text-gray-400" />
        </div>
        <p className="text-sm text-gray-500">Filing 정보를 불러올 수 없습니다</p>
        <p className="text-xs text-gray-400">잠시 후 다시 시도해 주세요</p>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 px-4 py-16">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100">
          <FileText className="h-6 w-6 text-gray-300" />
        </div>
        <p className="text-sm text-gray-500">Filing 내역이 없습니다</p>
      </div>
    );
  }

  return (
    <div className="p-4 space-y-2">
      <div className="flex items-center justify-between mb-1">
        <p className="text-[13px] text-gray-500">
          최근 Filing <span className="font-semibold text-gray-700">{data.length}</span>건
        </p>
        <span className="text-[11px] text-gray-400">SEC EDGAR</span>
      </div>

      <div className="space-y-2">
        {data.map((item) => (
          <a
            key={item.id}
            href={item.edgarUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="group block rounded-xl bg-white p-4 transition-colors hover:bg-gray-50 active:bg-gray-100"
          >
            <div className="flex items-center gap-2 mb-1.5">
              <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-bold ${getDisclosureTypeBadgeClass(item.filingType)}`}>
                {item.filingType}
              </span>
              <span className="flex items-center gap-1 text-[11px] text-gray-400">
                <Calendar className="h-3 w-3" />
                {formatDate(item.filingDate)}
              </span>
            </div>
            <p className="text-[13px] font-medium text-gray-900 leading-snug line-clamp-2 group-hover:text-[#3182F6] transition-colors">
              {item.title}
            </p>
            <div className="mt-1.5 flex items-center justify-end">
              <ExternalLink className="h-3.5 w-3.5 text-gray-300 group-hover:text-[#3182F6] transition-colors" />
            </div>
          </a>
        ))}
      </div>
    </div>
  );
}

/* ────────────────────────────────────────────────────────
 * CommunityTab - Stock discussion board
 * ──────────────────────────────────────────────────────── */

function CommunityTab({ stockCode }: { stockCode: string }) {
  return (
    <div className="space-y-3 p-4">
      {/* Disclaimer banner */}
      <div className="flex items-start gap-2.5 rounded-2xl bg-amber-50/70 px-4 py-3">
        <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-amber-500" />
        <p className="text-[12px] text-amber-700 leading-relaxed">
          이 공간은 투자자 의견 공유 목적이며, 투자 권유가 아닙니다.
        </p>
      </div>

      {/* Post write form */}
      <PostWriteForm stockCode={stockCode} />

      {/* Post list */}
      <PostList stockCode={stockCode} />
    </div>
  );
}
