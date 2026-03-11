'use client';

import { TrendingUp } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { useStockFundamentalsHistory } from '@/hooks/use-stocks';

function formatBarLabel(value: number): string {
  const absValue = Math.abs(value);
  const sign = value < 0 ? '-' : '';
  const trillion = 1_000_000_000_000;
  const billion = 100_000_000;

  if (absValue >= trillion) {
    return `${sign}${(absValue / trillion).toFixed(1)}조`;
  }
  if (absValue >= billion) {
    const billionValue = absValue / billion;
    if (billionValue >= 1000) {
      return `${sign}${(billionValue / 10000).toFixed(1)}조`;
    }
    return `${sign}${Math.round(billionValue).toLocaleString('ko-KR')}억`;
  }
  return `${sign}${Math.round(absValue / 10000).toLocaleString('ko-KR')}만`;
}

function TrendChartSkeleton() {
  return (
    <Card className="rounded-xl border-0 bg-white shadow-none">
      <CardContent className="p-4">
        <div className="mb-4 flex items-center gap-2">
          <Skeleton className="h-4 w-4 rounded" />
          <Skeleton className="h-4 w-24" />
        </div>
        <div className="flex items-end justify-around gap-3 h-48">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="flex flex-col items-center gap-1 flex-1">
              <Skeleton className="w-full rounded-t" style={{ height: `${40 + i * 20}%` }} />
              <Skeleton className="h-3 w-10" />
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

interface FinancialTrendChartProps {

  stockCode: string;
}

export function FinancialTrendChart({ stockCode }: FinancialTrendChartProps) {
  const { data: history, isLoading, isFetching, isError } = useStockFundamentalsHistory(stockCode);

  if (isLoading || (isError && isFetching)) {
    return <TrendChartSkeleton />;
  }

  if (isError || !history || !history.yearlyData || history.yearlyData.length === 0) {
    return (
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <div className="mb-3 flex items-center gap-2">
            <TrendingUp className="h-4 w-4 text-[#3182F6]" />
            <h2 className="text-sm font-semibold text-gray-900">
              재무 추이
            </h2>
          </div>
          <p className="py-4 text-center text-sm text-gray-400">
            다년도 재무 데이터가 아직 없습니다
          </p>
        </CardContent>
      </Card>
    );
  }

  const { yearlyData } = history;

  const allRevenues = yearlyData
    .map((d) => d.revenue)
    .filter((v): v is number => v !== null);
  const allProfits = yearlyData
    .map((d) => d.operatingProfit)
    .filter((v): v is number => v !== null);

  const maxRevenue = allRevenues.length > 0 ? Math.max(...allRevenues.map(Math.abs)) : 0;
  const maxProfit = allProfits.length > 0 ? Math.max(...allProfits.map(Math.abs)) : 0;

  const maxValue = Math.max(maxRevenue, maxProfit, 1);


  const BAR_AREA_HEIGHT = 160;


  function getBarHeight(value: number | null): number {
    if (value === null || maxValue === 0) return 0;
    const ratio = Math.abs(value) / maxValue;
    return Math.max(ratio * BAR_AREA_HEIGHT, 4);
  }

  return (
    <Card className="rounded-xl border-0 bg-white shadow-none">
      <CardContent className="p-4">

        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <TrendingUp className="h-4 w-4 text-[#3182F6]" />
            <h2 className="text-sm font-semibold text-gray-900">
              재무 추이
            </h2>
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-1">
              <div className="h-2.5 w-2.5 rounded-sm bg-[#3182F6]" />
              <span className="text-[10px] text-gray-500">매출액</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="h-2.5 w-2.5 rounded-sm bg-[#00B894]" />
              <span className="text-[10px] text-gray-500">영업이익</span>
            </div>
          </div>
        </div>

        <div
          className="flex items-end justify-around gap-2 sm:gap-4"
          style={{ height: `${BAR_AREA_HEIGHT + 40}px` }}
          role="img"
          aria-label={`${history.stockName} 재무 추이 차트: ${yearlyData.map((d) => d.bsnsYear).join(', ')}년`}
        >
          {yearlyData.map((yearData) => {
            const revenueHeight = getBarHeight(yearData.revenue);
            const profitHeight = getBarHeight(yearData.operatingProfit);
            const isNegativeProfit =
              yearData.operatingProfit !== null && yearData.operatingProfit < 0;

            return (
              <div
                key={yearData.bsnsYear}
                className="flex flex-col items-center flex-1 min-w-0"
              >

                <div className="flex gap-0.5 mb-1 min-h-[28px] items-end">
                  {yearData.revenue !== null && (
                    <span className="text-[9px] font-medium text-[#3182F6] tabular-nums whitespace-nowrap">
                      {formatBarLabel(yearData.revenue)}
                    </span>
                  )}
                </div>

                <div
                  className="w-full flex items-end justify-center gap-1"
                  style={{ height: `${BAR_AREA_HEIGHT}px` }}
                >

                  {yearData.revenue !== null ? (
                    <div
                      className="flex-1 max-w-[28px] bg-[#3182F6] rounded-t-md transition-all duration-700 ease-out"
                      style={{ height: `${revenueHeight}px` }}
                      title={`매출액: ${formatBarLabel(yearData.revenue)}`}
                    />
                  ) : (
                    <div className="flex-1 max-w-[28px]" />
                  )}

                  {yearData.operatingProfit !== null ? (
                    <div className="flex flex-col items-center flex-1 max-w-[28px] justify-end h-full">
                      <div
                        className={`w-full rounded-t-md transition-all duration-700 ease-out ${
                          isNegativeProfit ? 'bg-[#F04452]' : 'bg-[#00B894]'
                        }`}
                        style={{ height: `${profitHeight}px` }}
                        title={`영업이익: ${formatBarLabel(yearData.operatingProfit)}`}
                      />
                    </div>
                  ) : (
                    <div className="flex-1 max-w-[28px]" />
                  )}
                </div>

                <div className="mt-0.5 min-h-[14px]">
                  {yearData.operatingProfit !== null && (
                    <span
                      className={`text-[9px] font-medium tabular-nums whitespace-nowrap ${
                        isNegativeProfit ? 'text-[#F04452]' : 'text-[#00B894]'
                      }`}
                    >
                      {formatBarLabel(yearData.operatingProfit)}
                    </span>
                  )}
                </div>

                <span className="mt-1 text-[11px] font-medium text-gray-500">
                  {yearData.bsnsYear}
                </span>
              </div>
            );
          })}
        </div>

        <p className="mt-3 text-[10px] text-gray-400">
          출처: DART 전자공시시스템
        </p>
      </CardContent>
    </Card>
  );
}
