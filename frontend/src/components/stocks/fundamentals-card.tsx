'use client';

import {
  TrendingUp,
  TrendingDown,
  HelpCircle,
  CheckCircle,
  XCircle,
  BarChart3,
  AlertTriangle,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Skeleton } from '@/components/ui/skeleton';
import { useStockFundamentals } from '@/hooks/use-stocks';
import { cn } from '@/lib/utils';

const FUNDAMENTALS_EXPLANATIONS: Record<string, string> = {
  '매출액':
    '회사가 1년 동안 상품이나 서비스를 팔아 벌어들인 총 금액입니다. 매출이 클수록 사업 규모가 큰 기업입니다.',
  '영업이익률':
    '매출액 중에서 실제로 영업활동으로 남긴 이익의 비율입니다. 높을수록 본업에서 돈을 잘 버는 기업입니다. 보통 10% 이상이면 양호합니다.',
  '매출성장률':
    '전년 대비 매출이 얼마나 늘었는지(또는 줄었는지)를 나타냅니다. 양수이면 성장 중, 음수이면 매출이 줄어들고 있는 기업입니다.',
  '부채비율':
    '회사의 자본 대비 빚의 비율입니다. 낮을수록 재무적으로 안정적입니다. 200%를 넘으면 빚이 많아 주의가 필요합니다.',
  ROE: '자기자본이익률. 주주가 투자한 돈으로 얼마나 이익을 냈는지를 나타냅니다. 10% 이상이면 양호, 5% 미만이면 수익성이 낮은 편입니다.',
  '영업현금흐름':
    '실제 영업활동으로 현금이 들어오는지, 나가는지를 보여줍니다. 양수이면 현금이 잘 들어오는 건강한 기업, 음수이면 현금이 부족한 상태입니다.',
};

function formatLargeAmount(value: number): string {
  const absValue = Math.abs(value);
  const sign = value < 0 ? '-' : '';
  const trillion = 1_000_000_000_000;
  const billion = 100_000_000;

  if (absValue >= trillion) {
    const trillionPart = Math.floor(absValue / trillion);
    const billionPart = Math.floor((absValue % trillion) / billion);
    if (billionPart > 0) {
      return `${sign}${trillionPart.toLocaleString('ko-KR')}조 ${billionPart.toLocaleString('ko-KR')}억`;
    }
    return `${sign}${trillionPart.toLocaleString('ko-KR')}조`;
  }

  if (absValue >= billion) {
    return `${sign}${Math.floor(absValue / billion).toLocaleString('ko-KR')}억`;
  }

  return `${sign}${absValue.toLocaleString('ko-KR')}원`;
}

function FundamentalMetricRow({
  label,
  value,
  valueColorClass,
  icon,
}: {
  label: string;
  value: React.ReactNode;
  valueColorClass?: string;
  icon?: React.ReactNode;
}) {
  return (
    <div className="flex items-center justify-between py-2.5">
      <div className="flex items-center gap-1">
        <span className="text-sm text-gray-500">{label}</span>
        {FUNDAMENTALS_EXPLANATIONS[label] && (
          <Popover>
            <PopoverTrigger asChild>
              <button
                className="text-gray-400 hover:text-gray-600 transition-colors"
                aria-label={`${label} 설명 보기`}
              >
                <HelpCircle className="h-3.5 w-3.5" />
              </button>
            </PopoverTrigger>
            <PopoverContent className="w-64 p-3 text-sm" align="start">
              <p className="font-medium text-gray-900 mb-1">{label}</p>
              <p className="text-gray-600 text-xs leading-relaxed">
                {FUNDAMENTALS_EXPLANATIONS[label]}
              </p>
            </PopoverContent>
          </Popover>
        )}
      </div>
      <div className="flex items-center gap-1.5">
        {icon}
        <span
          className={cn(
            'text-sm font-medium tabular-nums',
            valueColorClass || 'text-gray-900'
          )}
        >
          {value}
        </span>
      </div>
    </div>
  );
}

function FundamentalsSkeleton() {
  return (
    <Card className="rounded-xl border-0 bg-white shadow-none">
      <CardContent className="p-4">
        <div className="mb-3 flex items-center gap-2">
          <Skeleton className="h-4 w-4 rounded" />
          <Skeleton className="h-4 w-24" />
        </div>
        <div className="space-y-1">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="flex items-center justify-between py-2.5">
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-4 w-24" />
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

interface FundamentalsCardProps {

  stockCode: string;
}

export function FundamentalsCard({ stockCode }: FundamentalsCardProps) {
  const { data: fundamentals, isLoading, isError } = useStockFundamentals(stockCode);

  if (isLoading) {
    return <FundamentalsSkeleton />;
  }

  if (isError) {
    return (
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <div className="mb-3 flex items-center gap-2">
            <BarChart3 className="h-4 w-4 text-[#3182F6]" />
            <h2 className="text-sm font-semibold text-gray-900">
              기업 가치분석
            </h2>
          </div>
          <p className="py-4 text-center text-sm text-gray-400">
            재무 데이터를 불러올 수 없습니다
          </p>
        </CardContent>
      </Card>
    );
  }

  if (!fundamentals) {
    return (
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <div className="mb-3 flex items-center gap-2">
            <BarChart3 className="h-4 w-4 text-[#3182F6]" />
            <h2 className="text-sm font-semibold text-gray-900">
              기업 가치분석
            </h2>
          </div>
          <p className="py-4 text-center text-sm text-gray-400">
            해외 상장 종목은 DART 재무제표가 제공되지 않습니다
          </p>
        </CardContent>
      </Card>
    );
  }



  const operatingMarginColor =
    fundamentals.operatingMargin !== null
      ? fundamentals.operatingMargin > 0
        ? 'text-[#F04452]'
        : fundamentals.operatingMargin < 0
        ? 'text-[#3182F6]'
        : 'text-gray-900'
      : 'text-gray-400';

  const revenueGrowthColor =
    fundamentals.revenueGrowthRate !== null
      ? fundamentals.revenueGrowthRate > 0
        ? 'text-[#F04452]'
        : fundamentals.revenueGrowthRate < 0
        ? 'text-[#3182F6]'
        : 'text-gray-900'
      : 'text-gray-400';

  const revenueGrowthIcon =
    fundamentals.revenueGrowthRate !== null ? (
      fundamentals.revenueGrowthRate > 0 ? (
        <TrendingUp className="h-3.5 w-3.5 text-[#F04452]" />
      ) : fundamentals.revenueGrowthRate < 0 ? (
        <TrendingDown className="h-3.5 w-3.5 text-[#3182F6]" />
      ) : null
    ) : null;

  const debtRatioColor =
    fundamentals.debtRatio !== null
      ? fundamentals.debtRatio > 200
        ? 'text-[#F59E0B]'
        : 'text-gray-900'
      : 'text-gray-400';

  const debtRatioIcon =
    fundamentals.debtRatio !== null && fundamentals.debtRatio > 200 ? (
      <AlertTriangle className="h-3.5 w-3.5 text-[#F59E0B]" />
    ) : null;

  const roeColor =
    fundamentals.roe !== null
      ? fundamentals.roe >= 10
        ? 'text-[#00B894]'
        : fundamentals.roe < 5
        ? 'text-[#F59E0B]'
        : 'text-gray-900'
      : 'text-gray-400';

  const cashFlowColor =
    fundamentals.operatingCashFlow !== null
      ? fundamentals.operatingCashFlow > 0
        ? 'text-[#00B894]'
        : 'text-[#F04452]'
      : 'text-gray-400';

  const cashFlowIcon =
    fundamentals.operatingCashFlow !== null ? (
      fundamentals.operatingCashFlow > 0 ? (
        <CheckCircle className="h-3.5 w-3.5 text-[#00B894]" />
      ) : (
        <XCircle className="h-3.5 w-3.5 text-[#F04452]" />
      )
    ) : null;

  return (
    <Card className="rounded-xl border-0 bg-white shadow-none">
      <CardContent className="p-4">

        <div className="mb-2 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <BarChart3 className="h-4 w-4 text-[#3182F6]" />
            <h2 className="text-sm font-semibold text-gray-900">
              기업 가치분석
            </h2>
          </div>
          <span className="text-[10px] text-gray-400">
            {fundamentals.bsnsYear}년 기준
          </span>
        </div>

        <div className="divide-y divide-gray-100">

          <FundamentalMetricRow
            label="매출액"
            value={
              fundamentals.revenue !== null
                ? formatLargeAmount(fundamentals.revenue)
                : '-'
            }
          />

          <FundamentalMetricRow
            label="영업이익률"
            value={
              fundamentals.operatingMargin !== null
                ? `${fundamentals.operatingMargin > 0 ? '+' : ''}${fundamentals.operatingMargin.toFixed(1)}%`
                : '-'
            }
            valueColorClass={operatingMarginColor}
          />

          <FundamentalMetricRow
            label="매출성장률"
            value={
              fundamentals.revenueGrowthRate !== null
                ? `${fundamentals.revenueGrowthRate > 0 ? '+' : ''}${fundamentals.revenueGrowthRate.toFixed(1)}%`
                : '-'
            }
            valueColorClass={revenueGrowthColor}
            icon={revenueGrowthIcon}
          />

          <FundamentalMetricRow
            label="부채비율"
            value={
              fundamentals.debtRatio !== null
                ? `${fundamentals.debtRatio.toFixed(1)}%`
                : '-'
            }
            valueColorClass={debtRatioColor}
            icon={debtRatioIcon}
          />

          <FundamentalMetricRow
            label="ROE"
            value={
              fundamentals.roe !== null
                ? `${fundamentals.roe > 0 ? '+' : ''}${fundamentals.roe.toFixed(1)}%`
                : '-'
            }
            valueColorClass={roeColor}
          />

          <FundamentalMetricRow
            label="영업현금흐름"
            value={
              fundamentals.operatingCashFlow !== null
                ? formatLargeAmount(fundamentals.operatingCashFlow)
                : '-'
            }
            valueColorClass={cashFlowColor}
            icon={cashFlowIcon}
          />
        </div>

        <p className="mt-3 text-[10px] text-gray-400">
          출처: DART 전자공시시스템 ({fundamentals.bsnsYear} 사업연도)
        </p>
      </CardContent>
    </Card>
  );
}
