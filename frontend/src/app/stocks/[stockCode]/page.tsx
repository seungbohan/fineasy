'use client';

import { use, useState } from 'react';
import Link from 'next/link';
import { ArrowLeft, Heart, ExternalLink, Sparkles, TrendingUp, Tag, AlertCircle, Globe, Newspaper, BarChart3 } from 'lucide-react';
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
import { StockChart } from '@/components/stocks/stock-chart';
import { CompanyAnalysisCard } from '@/components/stocks/company-analysis-card';

import { FundamentalAnalysis } from '@/components/stocks/fundamental-analysis';
import { useStockDetail } from '@/hooks/use-stocks';
import { useStockNews, useNewsAnalysis } from '@/hooks/use-news';
import { useAnalysisReport } from '@/hooks/use-analysis';
import { PredictionCard } from '@/components/stocks/prediction-card';
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

  const watched = isWatched(stockCode);

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
            aria-label={
              watched ? '관심 종목 해제' : '관심 종목 추가'
            }
          >
            <Heart className={`h-6 w-6 ${watched ? 'fill-current' : ''}`} />
          </button>
        </div>
      </div>

      <div className="space-y-2 p-4">
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

        <div className="rounded-xl bg-white overflow-hidden">
          <StockChart stockCode={stockCode} market={stock.market} />
        </div>

        <CompanyAnalysisCard stockCode={stockCode} stock={stock} />

        <FundamentalAnalysis stockCode={stockCode} market={stock.market} />

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

        <PredictionCard stockCode={stockCode} />

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

              {analysisData.analysis.relatedStocks.length > 0 && (
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
