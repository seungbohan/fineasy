'use client';

import { BarChart3, Sparkles, Shield, Wallet, Award } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { useStockFundamentals, useStockFundamentalsHistory } from '@/hooks/use-stocks';
import { cn } from '@/lib/utils';
import type { DartFundamentals, MultiYearFundamentals } from '@/types';

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
  if (absValue >= 10000) {
    return `${sign}${Math.round(absValue / 10000).toLocaleString('ko-KR')}만`;
  }
  return `${sign}${absValue.toLocaleString('ko-KR')}`;
}

function FundamentalSkeleton() {
  return (
    <Card className="rounded-xl border-0 bg-white shadow-none">
      <CardContent className="p-4">
        <div className="mb-4 flex items-center gap-2">
          <Skeleton className="h-4 w-4 rounded" />
          <Skeleton className="h-4 w-28" />
        </div>
        <Skeleton className="h-9 w-full rounded-lg mb-4" />
        <div className="flex items-end justify-around gap-3 h-48">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="flex flex-col items-center gap-1 flex-1">
              <Skeleton className="w-full rounded-t" style={{ height: `${30 + i * 15}%` }} />
              <Skeleton className="h-3 w-10" />
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

const BAR_AREA_HEIGHT = 160;

function getBarHeight(value: number | null, maxValue: number): number {
  if (value === null || maxValue === 0) return 0;
  const ratio = Math.abs(value) / maxValue;
  return Math.max(ratio * BAR_AREA_HEIGHT, 4);
}

function ProfitabilityTab({ yearlyData }: { yearlyData: MultiYearFundamentals['yearlyData'] }) {
  const allValues = yearlyData.flatMap((d) =>
    [d.revenue, d.operatingProfit, d.netIncome].filter((v): v is number => v !== null)
  );
  const maxValue = allValues.length > 0 ? Math.max(...allValues.map(Math.abs)) : 1;

  return (
    <div>

      <div className="flex items-center gap-4 mb-4">
        <div className="flex items-center gap-1">
          <div className="h-2.5 w-2.5 rounded-sm bg-[#3182F6]" />
          <span className="text-[10px] text-gray-500">매출액</span>
        </div>
        <div className="flex items-center gap-1">
          <div className="h-2.5 w-2.5 rounded-sm bg-[#00B894]" />
          <span className="text-[10px] text-gray-500">영업이익</span>
        </div>
        <div className="flex items-center gap-1">
          <div className="h-2.5 w-2.5 rounded-sm bg-[#6C5CE7]" />
          <span className="text-[10px] text-gray-500">순이익</span>
        </div>
      </div>

      <div
        className="flex items-end justify-around gap-1 sm:gap-3"
        style={{ height: `${BAR_AREA_HEIGHT + 50}px` }}
        role="img"
        aria-label="수익성 추이 차트"
      >
        {yearlyData.map((yearData) => {
          const revenueH = getBarHeight(yearData.revenue, maxValue);
          const profitH = getBarHeight(yearData.operatingProfit, maxValue);
          const netIncomeH = getBarHeight(yearData.netIncome, maxValue);
          const isNegProfit = yearData.operatingProfit !== null && yearData.operatingProfit < 0;
          const isNegNet = yearData.netIncome !== null && yearData.netIncome < 0;

          return (
            <div key={yearData.bsnsYear} className="flex flex-col items-center flex-1 min-w-0">

              <div className="flex gap-0.5 mb-1 min-h-[20px] items-end">
                {yearData.revenue !== null && (
                  <span className="text-[8px] font-medium text-[#3182F6] tabular-nums whitespace-nowrap">
                    {formatBarLabel(yearData.revenue)}
                  </span>
                )}
              </div>

              <div
                className="w-full flex items-end justify-center gap-0.5"
                style={{ height: `${BAR_AREA_HEIGHT}px` }}
              >
                {yearData.revenue !== null && (
                  <div
                    className="flex-1 max-w-[20px] bg-[#3182F6] rounded-t-sm transition-all duration-700 ease-out"
                    style={{ height: `${revenueH}px` }}
                    title={`매출액: ${formatBarLabel(yearData.revenue)}`}
                  />
                )}
                {yearData.operatingProfit !== null && (
                  <div
                    className={cn(
                      'flex-1 max-w-[20px] rounded-t-sm transition-all duration-700 ease-out',
                      isNegProfit ? 'bg-[#F04452]' : 'bg-[#00B894]'
                    )}
                    style={{ height: `${profitH}px` }}
                    title={`영업이익: ${formatBarLabel(yearData.operatingProfit)}`}
                  />
                )}
                {yearData.netIncome !== null && (
                  <div
                    className={cn(
                      'flex-1 max-w-[20px] rounded-t-sm transition-all duration-700 ease-out',
                      isNegNet ? 'bg-[#F04452]/70' : 'bg-[#6C5CE7]'
                    )}
                    style={{ height: `${netIncomeH}px` }}
                    title={`순이익: ${formatBarLabel(yearData.netIncome)}`}
                  />
                )}
              </div>

              <span className="mt-2 text-[11px] font-medium text-gray-500">
                {yearData.bsnsYear}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function StabilityTab({
  yearlyData,
  fundamentals,
}: {
  yearlyData: MultiYearFundamentals['yearlyData'];
  fundamentals: DartFundamentals | null;
}) {
  const debtRatios = yearlyData
    .map((d) => d.debtRatio)
    .filter((v): v is number => v !== null);
  const maxDebtRatio = debtRatios.length > 0 ? Math.max(...debtRatios, 100) : 200;

  const hasBalanceSheet =
    fundamentals &&
    fundamentals.totalAssets !== null &&
    fundamentals.totalLiabilities !== null &&
    fundamentals.totalEquity !== null;

  return (
    <div className="space-y-6">
      <div>
        <h4 className="text-xs font-semibold text-gray-500 mb-3">부채비율 추이</h4>
        <div className="space-y-2">
          {yearlyData.map((yearData) => {
            if (yearData.debtRatio === null) return null;
            const width = Math.min((yearData.debtRatio / maxDebtRatio) * 100, 100);
            const isHigh = yearData.debtRatio > 200;

            return (
              <div key={yearData.bsnsYear} className="flex items-center gap-2">
                <span className="w-10 text-[11px] font-medium text-gray-500 shrink-0">
                  {yearData.bsnsYear}
                </span>
                <div className="flex-1 h-5 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className={cn(
                      'h-full rounded-full transition-all duration-700 ease-out',
                      isHigh ? 'bg-[#F59E0B]' : 'bg-[#3182F6]'
                    )}
                    style={{ width: `${width}%` }}
                  />
                </div>
                <span
                  className={cn(
                    'w-14 text-right text-xs font-medium tabular-nums',
                    isHigh ? 'text-[#F59E0B]' : 'text-gray-900'
                  )}
                >
                  {yearData.debtRatio.toFixed(1)}%
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {hasBalanceSheet && (
        <div>
          <h4 className="text-xs font-semibold text-gray-500 mb-3">
            자산 구조 ({fundamentals.bsnsYear}년)
          </h4>
          <BalanceStructureBar
            totalAssets={fundamentals.totalAssets!}
            totalLiabilities={fundamentals.totalLiabilities!}
            totalEquity={fundamentals.totalEquity!}
          />
        </div>
      )}
    </div>
  );
}

function BalanceStructureBar({
  totalAssets,
  totalLiabilities,
  totalEquity,
}: {
  totalAssets: number;
  totalLiabilities: number;
  totalEquity: number;
}) {
  const total = Math.abs(totalLiabilities) + Math.abs(totalEquity);
  if (total === 0) return null;

  const liabPct = (Math.abs(totalLiabilities) / total) * 100;
  const equityPct = (Math.abs(totalEquity) / total) * 100;

  return (
    <div>
      <div className="flex rounded-full overflow-hidden h-6 mb-2">
        <div
          className="bg-[#F04452]/80 flex items-center justify-center text-[9px] font-medium text-white transition-all duration-700"
          style={{ width: `${liabPct}%` }}
        >
          {liabPct > 15 && `${liabPct.toFixed(0)}%`}
        </div>
        <div
          className="bg-[#3182F6] flex items-center justify-center text-[9px] font-medium text-white transition-all duration-700"
          style={{ width: `${equityPct}%` }}
        >
          {equityPct > 15 && `${equityPct.toFixed(0)}%`}
        </div>
      </div>
      <div className="flex items-center gap-4 text-[10px]">
        <div className="flex items-center gap-1">
          <div className="h-2 w-2 rounded-sm bg-[#F04452]/80" />
          <span className="text-gray-500">
            부채 {formatBarLabel(totalLiabilities)}
          </span>
        </div>
        <div className="flex items-center gap-1">
          <div className="h-2 w-2 rounded-sm bg-[#3182F6]" />
          <span className="text-gray-500">
            자본 {formatBarLabel(totalEquity)}
          </span>
        </div>
      </div>
      <p className="mt-1 text-[10px] text-gray-400">
        자산총계: {formatBarLabel(totalAssets)}
      </p>
    </div>
  );
}

function CashFlowTab({
  yearlyData,
  fundamentals,
}: {
  yearlyData: MultiYearFundamentals['yearlyData'];
  fundamentals: DartFundamentals | null;
}) {
  const hasCashFlowHistory = yearlyData.some(
    (d) => d.operatingCashFlow !== null && d.operatingCashFlow !== undefined
  );

  if (hasCashFlowHistory) {
    const cashFlows = yearlyData
      .map((d) => d.operatingCashFlow)
      .filter((v): v is number => v !== null && v !== undefined);
    const maxCf = cashFlows.length > 0 ? Math.max(...cashFlows.map(Math.abs)) : 1;

    return (
      <div>
        <h4 className="text-xs font-semibold text-gray-500 mb-3">영업활동 현금흐름 추이</h4>
        <div className="space-y-2">
          {yearlyData.map((yearData) => {
            const cf = yearData.operatingCashFlow;
            if (cf === null || cf === undefined) return null;

            const width = Math.min((Math.abs(cf) / maxCf) * 100, 100);
            const isPositive = cf >= 0;

            return (
              <div key={yearData.bsnsYear} className="flex items-center gap-2">
                <span className="w-10 text-[11px] font-medium text-gray-500 shrink-0">
                  {yearData.bsnsYear}
                </span>
                <div className="flex-1 h-5 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className={cn(
                      'h-full rounded-full transition-all duration-700 ease-out',
                      isPositive ? 'bg-[#00B894]' : 'bg-[#F04452]'
                    )}
                    style={{ width: `${width}%` }}
                  />
                </div>
                <span
                  className={cn(
                    'w-16 text-right text-xs font-medium tabular-nums',
                    isPositive ? 'text-[#00B894]' : 'text-[#F04452]'
                  )}
                >
                  {formatBarLabel(cf)}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    );
  }

  if (fundamentals?.operatingCashFlow !== null && fundamentals?.operatingCashFlow !== undefined) {
    const cf = fundamentals.operatingCashFlow;
    const isPositive = cf >= 0;
    return (
      <div>
        <h4 className="text-xs font-semibold text-gray-500 mb-3">
          영업활동 현금흐름 ({fundamentals.bsnsYear}년)
        </h4>
        <div className="flex items-center gap-3 py-2">
          <Wallet className={cn('h-5 w-5', isPositive ? 'text-[#00B894]' : 'text-[#F04452]')} />
          <span
            className={cn(
              'text-lg font-bold tabular-nums',
              isPositive ? 'text-[#00B894]' : 'text-[#F04452]'
            )}
          >
            {formatBarLabel(cf)}
          </span>
        </div>
        <p className="text-xs text-gray-500 leading-relaxed mt-1">
          {isPositive
            ? '영업활동에서 현금이 유입되고 있어 건강한 상태입니다.'
            : '영업활동에서 현금이 유출되고 있어 주의가 필요합니다.'}
        </p>
      </div>
    );
  }

  return (
    <p className="py-6 text-center text-sm text-gray-400">
      현금흐름 데이터가 없습니다
    </p>
  );
}

function EvaluationTab({ fundamentals }: { fundamentals: DartFundamentals | null }) {
  if (!fundamentals) {
    return (
      <p className="py-6 text-center text-sm text-gray-400">
        종합평가 데이터가 없습니다
      </p>
    );
  }

  const tags = fundamentals.evaluationTags ?? [];
  const comment = fundamentals.summaryComment;

  if (tags.length === 0 && !comment) {
    return (
      <p className="py-6 text-center text-sm text-gray-400">
        AI 종합평가가 아직 생성되지 않았습니다
      </p>
    );
  }

  return (
    <div className="space-y-4">

      {tags.length > 0 && (
        <div>
          <h4 className="text-xs font-semibold text-gray-500 mb-2">평가 태그</h4>
          <div className="flex flex-wrap gap-1.5">
            {tags.map((tag, idx) => {
              const color = getTagColor(tag);
              return (
                <span
                  key={idx}
                  className={cn(
                    'rounded-full px-3 py-1.5 text-[11px] font-medium',
                    color.bg,
                    color.text
                  )}
                >
                  {tag}
                </span>
              );
            })}
          </div>
        </div>
      )}

      {comment && (
        <div>
          <h4 className="text-xs font-semibold text-gray-500 mb-2">AI 종합 코멘트</h4>
          <div className="bg-blue-50/50 rounded-xl p-3">
            <p className="text-sm text-gray-700 leading-relaxed">
              {comment}
            </p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-2 gap-3">
        {fundamentals.operatingMargin !== null && (
          <MetricCard
            label="영업이익률"
            value={`${fundamentals.operatingMargin > 0 ? '+' : ''}${fundamentals.operatingMargin.toFixed(1)}%`}
            isPositive={fundamentals.operatingMargin > 0}
          />
        )}
        {fundamentals.roe !== null && (
          <MetricCard
            label="ROE"
            value={`${fundamentals.roe > 0 ? '+' : ''}${fundamentals.roe.toFixed(1)}%`}
            isPositive={fundamentals.roe >= 10}
          />
        )}
        {fundamentals.debtRatio !== null && (
          <MetricCard
            label="부채비율"
            value={`${fundamentals.debtRatio.toFixed(1)}%`}
            isPositive={fundamentals.debtRatio <= 200}
          />
        )}
        {fundamentals.revenueGrowthRate !== null && (
          <MetricCard
            label="매출성장률"
            value={`${fundamentals.revenueGrowthRate > 0 ? '+' : ''}${fundamentals.revenueGrowthRate.toFixed(1)}%`}
            isPositive={fundamentals.revenueGrowthRate > 0}
          />
        )}
      </div>

      <p className="text-[10px] text-gray-400">
        이 평가는 AI가 공시 데이터를 기반으로 생성한 참고 자료이며, 투자 권유가 아닙니다
      </p>
    </div>
  );
}

function MetricCard({
  label,
  value,
  isPositive,
}: {
  label: string;
  value: string;
  isPositive: boolean;
}) {
  return (
    <div className="rounded-xl bg-gray-50 p-3">
      <p className="text-[10px] text-gray-500 mb-1">{label}</p>
      <p
        className={cn(
          'text-sm font-bold tabular-nums',
          isPositive ? 'text-[#00B894]' : 'text-[#F59E0B]'
        )}
      >
        {value}
      </p>
    </div>
  );
}

function getTagColor(tag: string): { bg: string; text: string } {
  const positiveKeywords = ['양호', '안정', '성장', '우수', '개선', '흑자', '증가'];
  const cautionKeywords = ['주의', '위험', '과다', '악화', '적자', '감소', '부진', '고평가'];

  if (positiveKeywords.some((kw) => tag.includes(kw))) {
    return { bg: 'bg-emerald-50', text: 'text-[#00B894]' };
  }
  if (cautionKeywords.some((kw) => tag.includes(kw))) {
    return { bg: 'bg-amber-50', text: 'text-[#F59E0B]' };
  }
  return { bg: 'bg-gray-50', text: 'text-gray-600' };
}

interface FundamentalAnalysisProps {

  stockCode: string;

  market: 'KRX' | 'KOSDAQ' | 'NASDAQ' | 'NYSE';
}

export function FundamentalAnalysis({ stockCode, market }: FundamentalAnalysisProps) {
  const isOverseas = market === 'NASDAQ' || market === 'NYSE';

  const {
    data: history,
    isLoading: historyLoading,
  } = useStockFundamentalsHistory(stockCode);

  const {
    data: fundamentals,
    isLoading: fundLoading,
  } = useStockFundamentals(stockCode);

  const isLoading = historyLoading || fundLoading;

  if (isOverseas) {
    return (
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <div className="mb-3 flex items-center gap-2">
            <BarChart3 className="h-4 w-4 text-[#3182F6]" />
            <h2 className="text-[15px] font-bold text-gray-900">
              펀더멘탈 분석
            </h2>
          </div>
          <p className="py-6 text-center text-sm text-gray-400">
            국내 상장 종목만 재무 데이터를 제공합니다
          </p>
        </CardContent>
      </Card>
    );
  }

  if (isLoading) {
    return <FundamentalSkeleton />;
  }

  const hasHistory = history?.yearlyData && history.yearlyData.length > 0;
  if (!hasHistory && !fundamentals) {
    return (
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <div className="mb-3 flex items-center gap-2">
            <BarChart3 className="h-4 w-4 text-[#3182F6]" />
            <h2 className="text-[15px] font-bold text-gray-900">
              펀더멘탈 분석
            </h2>
          </div>
          <p className="py-6 text-center text-sm text-gray-400">
            재무 데이터가 아직 없습니다
          </p>
        </CardContent>
      </Card>
    );
  }

  const yearlyData = history?.yearlyData ?? [];

  return (
    <Card className="rounded-xl border-0 bg-white shadow-none">
      <CardContent className="p-4">

        <div className="mb-3 flex items-center gap-2">
          <BarChart3 className="h-4 w-4 text-[#3182F6]" />
          <h2 className="text-[15px] font-bold text-gray-900">
            펀더멘탈 분석
          </h2>
          <span className="rounded-full bg-blue-50 px-2.5 py-0.5 text-[11px] font-semibold text-[#3182F6]">
            AI
          </span>
          {fundamentals?.bsnsYear && (
            <span className="ml-auto text-[10px] text-gray-400">
              {fundamentals.bsnsYear}년 기준
            </span>
          )}
        </div>

        <Tabs defaultValue="profitability">
          <TabsList className="w-full">
            <TabsTrigger value="profitability" className="flex-1 text-xs gap-1">
              <TrendingUpIcon className="h-3 w-3" />
              수익성
            </TabsTrigger>
            <TabsTrigger value="stability" className="flex-1 text-xs gap-1">
              <Shield className="h-3 w-3" />
              안정성
            </TabsTrigger>
            <TabsTrigger value="cashflow" className="flex-1 text-xs gap-1">
              <Wallet className="h-3 w-3" />
              현금흐름
            </TabsTrigger>
            <TabsTrigger value="evaluation" className="flex-1 text-xs gap-1">
              <Award className="h-3 w-3" />
              종합평가
            </TabsTrigger>
          </TabsList>

          <TabsContent value="profitability" className="mt-4">
            {hasHistory ? (
              <ProfitabilityTab yearlyData={yearlyData} />
            ) : (
              <p className="py-6 text-center text-sm text-gray-400">
                다년도 수익성 데이터가 없습니다
              </p>
            )}
          </TabsContent>

          <TabsContent value="stability" className="mt-4">
            {hasHistory ? (
              <StabilityTab yearlyData={yearlyData} fundamentals={fundamentals ?? null} />
            ) : (
              <p className="py-6 text-center text-sm text-gray-400">
                안정성 데이터가 없습니다
              </p>
            )}
          </TabsContent>

          <TabsContent value="cashflow" className="mt-4">
            <CashFlowTab
              yearlyData={yearlyData}
              fundamentals={fundamentals ?? null}
            />
          </TabsContent>

          <TabsContent value="evaluation" className="mt-4">
            <EvaluationTab fundamentals={fundamentals ?? null} />
          </TabsContent>
        </Tabs>

        <p className="mt-4 text-[10px] text-gray-400">
          출처: DART 전자공시시스템
        </p>
      </CardContent>
    </Card>
  );
}

function TrendingUpIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <polyline points="22 7 13.5 15.5 8.5 10.5 2 17" />
      <polyline points="16 7 22 7 22 13" />
    </svg>
  );
}
