'use client';

import { useState } from 'react';
import { Search, Bot, TrendingUp, TrendingDown, Minus, AlertTriangle } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Skeleton } from '@/components/ui/skeleton';
import { useStockSearch } from '@/hooks/use-stocks';
import { useAnalysisReport, usePrediction } from '@/hooks/use-analysis';
import { useMarketSummary } from '@/hooks/use-market';
import { formatPrice, formatChangeRate, getPriceColorClass } from '@/lib/format';
import { cn } from '@/lib/utils';

export default function AnalysisPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStock, setSelectedStock] = useState('');
  const [showResults, setShowResults] = useState(false);
  const [predictionPeriod, setPredictionPeriod] = useState<'1D' | '1W'>('1D');

  const { data: searchResults } = useStockSearch(searchQuery);
  const { data: report, isLoading: reportLoading } = useAnalysisReport(selectedStock);
  const { data: prediction, isLoading: predictionLoading } = usePrediction(selectedStock, predictionPeriod);
  const { data: marketSummary } = useMarketSummary();

  const handleSelectStock = (stockCode: string) => {
    setSelectedStock(stockCode);
    setSearchQuery('');
    setShowResults(false);
  };

  const selectedStockInfo = searchResults?.find(
    (s) => s.stockCode === selectedStock
  );

  return (
    <div className="mx-auto max-w-screen-xl p-4 md:p-6 space-y-4">
      <h1 className="text-xl font-bold text-gray-900">AI 주가 분석</h1>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <Input
          type="text"
          placeholder="종목을 검색하여 분석을 시작하세요"
          value={searchQuery}
          onChange={(e) => {
            setSearchQuery(e.target.value);
            setShowResults(true);
          }}
          className="h-10 pl-10 bg-white"
          aria-label="종목 검색"
          onFocus={() => setShowResults(true)}
        />

        {showResults && searchQuery && searchResults && searchResults.length > 0 && (
          <div className="absolute left-0 right-0 top-full z-10 mt-1 rounded-lg border bg-white shadow-lg">
            {searchResults.map((stock) => (
              <button
                key={stock.stockCode}
                onClick={() => handleSelectStock(stock.stockCode)}
                className="flex w-full items-center justify-between px-4 py-3 text-sm hover:bg-gray-50 transition-colors"
              >
                <div>
                  <span className="font-medium">{stock.stockName}</span>
                  <span className="ml-2 text-xs text-gray-400">
                    {stock.stockCode}
                  </span>
                </div>
                <div className="text-right tabular-nums">
                  <span className="text-sm">
                    {formatPrice(stock.currentPrice)}원
                  </span>
                  <span
                    className={`ml-2 text-xs ${getPriceColorClass(stock.changeRate)}`}
                  >
                    {formatChangeRate(stock.changeRate)}
                  </span>
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      {marketSummary && (
        <Card className="rounded-xl border-0 bg-white shadow-none">
          <CardContent className="p-4">
            <div className="flex items-center gap-2 mb-3">
              <Bot className="h-4 w-4 text-[#3182F6]" />
              <h2 className="text-sm font-semibold text-gray-900">
                오늘의 시장 분석
              </h2>
              <span className="rounded-full bg-blue-50 px-2 py-0.5 text-[10px] font-medium text-[#3182F6]">
                AI
              </span>
            </div>
            <p className="text-sm leading-relaxed text-gray-600">
              {marketSummary.summary}
            </p>
          </CardContent>
        </Card>
      )}

      {selectedStock && (
        <>
          <Card className="rounded-xl border-0 bg-white shadow-none">
            <CardContent className="p-4">
              <div className="flex items-center gap-2 mb-3">
                <Bot className="h-4 w-4 text-[#3182F6]" />
                <h2 className="text-sm font-semibold text-gray-900">
                  AI 분석 리포트
                </h2>
              </div>

              {reportLoading ? (
                <div className="space-y-3">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-5/6" />
                </div>
              ) : report ? (
                <div className="space-y-3">
                  <p className="text-sm font-medium text-gray-900">
                    {report.summary}
                  </p>
                  <p className="text-sm leading-relaxed text-gray-600">
                    {report.description}
                  </p>
                  <ul className="space-y-1.5">
                    {report.keyPoints.map((point, idx) => (
                      <li
                        key={idx}
                        className="flex items-start gap-2 text-sm text-gray-600"
                      >
                        <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-[#3182F6]" />
                        {point}
                      </li>
                    ))}
                  </ul>

                  <p className="text-[10px] text-gray-400 leading-relaxed">
                    {report.disclaimer}
                  </p>
                </div>
              ) : (
                <p className="py-4 text-center text-sm text-gray-400">
                  이 종목의 분석 데이터가 아직 없습니다
                </p>
              )}
            </CardContent>
          </Card>

          <Card className="rounded-xl border-0 bg-white shadow-none">
            <CardContent className="p-4">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <h2 className="text-sm font-semibold text-gray-900">
                    주가 예측
                  </h2>
                  <span className="rounded-full bg-yellow-50 px-2 py-0.5 text-[10px] font-medium text-yellow-700">
                    참고용
                  </span>
                </div>
              </div>

              <Tabs
                value={predictionPeriod}
                onValueChange={(value) => setPredictionPeriod(value as '1D' | '1W')}
                className="mb-4"
              >
                <TabsList className="h-8 w-full">
                  <TabsTrigger value="1D" className="flex-1 text-xs">
                    1거래일 후
                  </TabsTrigger>
                  <TabsTrigger value="1W" className="flex-1 text-xs">
                    1주일 후
                  </TabsTrigger>
                </TabsList>
              </Tabs>

              {predictionLoading ? (
                <div className="space-y-3">
                  <Skeleton className="h-16 w-full" />
                  <Skeleton className="h-4 w-3/4" />
                </div>
              ) : prediction ? (
                <div className="space-y-4">
                  <div className="flex items-center gap-4 rounded-xl bg-gray-50 p-4">
                    <div
                      className={cn(
                        'flex h-14 w-14 items-center justify-center rounded-full',
                        prediction.direction === 'UP'
                          ? 'bg-red-50'
                          : prediction.direction === 'DOWN'
                          ? 'bg-blue-50'
                          : 'bg-gray-100'
                      )}
                    >
                      {prediction.direction === 'UP' ? (
                        <TrendingUp className="h-6 w-6 text-up" />
                      ) : prediction.direction === 'DOWN' ? (
                        <TrendingDown className="h-6 w-6 text-down" />
                      ) : (
                        <Minus className="h-6 w-6 text-flat" />
                      )}
                    </div>
                    <div>
                      <p
                        className={cn(
                          'text-lg font-bold',
                          prediction.direction === 'UP'
                            ? 'text-up'
                            : prediction.direction === 'DOWN'
                            ? 'text-down'
                            : 'text-flat'
                        )}
                      >
                        {prediction.direction === 'UP'
                          ? '상승'
                          : prediction.direction === 'DOWN'
                          ? '하락'
                          : '보합'}{' '}
                        가능성 {prediction.confidence}%
                      </p>
                      <p className="text-xs text-gray-500">
                        예측 기간: {prediction.period === '1D' ? '1거래일' : '1주일'}
                      </p>
                    </div>
                  </div>

                  <div>
                    <p className="mb-2 text-xs font-medium text-gray-500">
                      주요 근거
                    </p>
                    <ul className="space-y-2">
                      {prediction.reasons.map((reason, idx) => (
                        <li
                          key={idx}
                          className="flex items-start gap-2 text-sm text-gray-600"
                        >
                          <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-gray-100 text-[10px] font-semibold text-gray-500">
                            {idx + 1}
                          </span>
                          {reason}
                        </li>
                      ))}
                    </ul>
                  </div>

                  <div className="flex items-start gap-2 rounded-lg bg-yellow-50 p-3">
                    <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-yellow-600" />
                    <p className="text-xs leading-relaxed text-yellow-800">
                      {prediction.disclaimer}
                    </p>
                  </div>
                </div>
              ) : (
                <p className="py-4 text-center text-sm text-gray-400">
                  이 종목의 예측 데이터가 아직 없습니다
                </p>
              )}
            </CardContent>
          </Card>
        </>
      )}

      {!selectedStock && (
        <div className="flex flex-col items-center gap-3 py-12 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-blue-50">
            <Bot className="h-7 w-7 text-[#3182F6]" />
          </div>
          <p className="text-sm font-medium text-gray-600">
            종목을 검색하여 AI 분석을 확인하세요
          </p>
          <p className="text-xs text-gray-400">
            AI 분석 리포트와 주가 방향성 예측을 제공합니다
          </p>
        </div>
      )}
    </div>
  );
}
