'use client';

import {
  TrendingUp,
  TrendingDown,
  HelpCircle,
  CheckCircle,
  XCircle,
  BarChart3,
  AlertTriangle,
  Building2,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Skeleton } from '@/components/ui/skeleton';
import { useStockFundamentals, useSectorComparison } from '@/hooks/use-stocks';
import { cn } from '@/lib/utils';
import { formatPrice, getCurrencyFromMarket } from '@/lib/format';
import type { Stock } from '@/types';

const METRIC_EXPLANATIONS: Record<string, string> = {
  PER: '주가수익비율. 현재 주가가 주당순이익의 몇 배인지 나타냅니다. 낮을수록 저평가, 높을수록 고평가로 볼 수 있습니다.',
  PBR: '주가순자산비율. 주가가 회사 순자산의 몇 배인지 나타냅니다. 1 이하이면 자산가치보다 주가가 낮은 저평가 상태입니다.',
  EPS: '주당순이익. 회사의 순이익을 주식 수로 나눈 값입니다. 높을수록 수익성이 좋은 기업입니다.',
  '시가총액':
    '기업의 전체 시장 가치입니다. 현재 주가에 발행 주식 수를 곱한 값입니다.',
  '배당수익률':
    '주식을 보유할 때 받을 수 있는 배당금의 비율입니다. 연간 배당금을 현재 주가로 나눈 값입니다.',
  '52주 최고': '최근 1년(52주) 동안 기록한 가장 높은 주가입니다.',
  '52주 최저': '최근 1년(52주) 동안 기록한 가장 낮은 주가입니다.',
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

function MetricRow({
  label,
  value,
  valueColorClass,
  icon,
  helpKey,
  suffix,
}: {
  label: string;
  value: React.ReactNode;
  valueColorClass?: string;
  icon?: React.ReactNode;
  helpKey?: string;
  suffix?: React.ReactNode;
}) {
  const explanationKey = helpKey || label;

  return (
    <div className="flex items-center justify-between py-2.5">
      <div className="flex items-center gap-1">
        <span className="text-sm text-gray-500">{label}</span>
        {METRIC_EXPLANATIONS[explanationKey] && (
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
                {METRIC_EXPLANATIONS[explanationKey]}
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
        {suffix}
      </div>
    </div>
  );
}

function ComparisonBar({
  label,
  currentValue,
  avgValue,
  evaluation,
  helpKey,
}: {
  label: string;
  currentValue: number | null;
  avgValue: number | null;
  evaluation: string;
  helpKey: string;
}) {
  if (currentValue === null || avgValue === null || avgValue === 0) return null;

  const maxVal = Math.max(Math.abs(currentValue), Math.abs(avgValue)) * 1.2;
  const currentWidth = Math.max((Math.abs(currentValue) / maxVal) * 100, 4);
  const avgWidth = Math.max((Math.abs(avgValue) / maxVal) * 100, 4);

  const evaluationColor =
    evaluation === '저평가'
      ? 'bg-[#00B894] text-[#00B894]'
      : evaluation === '고평가'
        ? 'bg-red-100 text-[#F04452]'
        : 'bg-gray-100 text-gray-600';

  const badgeBg =
    evaluation === '저평가'
      ? 'bg-emerald-50'
      : evaluation === '고평가'
        ? 'bg-red-50'
        : 'bg-gray-50';

  return (
    <div className="py-2.5">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-1">
          <span className="text-sm text-gray-500">{label}</span>
          {METRIC_EXPLANATIONS[helpKey] && (
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
                  {METRIC_EXPLANATIONS[helpKey]}
                </p>
              </PopoverContent>
            </Popover>
          )}
        </div>
        <span
          className={cn(
            'rounded-full px-2 py-0.5 text-[10px] font-medium',
            badgeBg,
            evaluationColor.split(' ')[1]
          )}
        >
          {evaluation}
        </span>
      </div>

      <div className="space-y-1.5">
        <div className="flex items-center gap-2">
          <span className="w-12 text-[10px] text-gray-400 shrink-0">현재</span>
          <div className="flex-1 h-4 bg-gray-100 rounded-full overflow-hidden">
            <div
              className="h-full bg-[#3182F6] rounded-full transition-all duration-500"
              style={{ width: `${currentWidth}%` }}
            />
          </div>
          <span className="w-14 text-right text-xs font-medium tabular-nums text-gray-900">
            {currentValue.toFixed(1)}배
          </span>
        </div>

        <div className="flex items-center gap-2">
          <span className="w-12 text-[10px] text-gray-400 shrink-0">업종</span>
          <div className="flex-1 h-4 bg-gray-100 rounded-full overflow-hidden">
            <div
              className="h-full bg-gray-300 rounded-full transition-all duration-500"
              style={{ width: `${avgWidth}%` }}
            />
          </div>
          <span className="w-14 text-right text-xs font-medium tabular-nums text-gray-500">
            {avgValue.toFixed(1)}배
          </span>
        </div>
      </div>
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

function SectionHeader({ title }: { title: string }) {
  return (
    <div className="flex items-center gap-1.5 pt-4 pb-1">
      <div className="h-1.5 w-1.5 rounded-full bg-[#3182F6]" />
      <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
        {title}
      </h3>
    </div>
  );
}

function CompanyAnalysisSkeleton() {
  return (
    <Card className="rounded-xl border-0 bg-white shadow-none">
      <CardContent className="p-4">
        <div className="mb-3 flex items-center gap-2">
          <Skeleton className="h-4 w-4 rounded" />
          <Skeleton className="h-4 w-24" />
        </div>
        <div className="space-y-1">
          {Array.from({ length: 10 }).map((_, i) => (
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

interface CompanyAnalysisCardProps {

  stockCode: string;

  stock: Stock;
}

export function CompanyAnalysisCard({ stockCode, stock }: CompanyAnalysisCardProps) {
  const { data: fundamentals, isLoading: fundLoading } = useStockFundamentals(stockCode);
  const { data: sectorData } = useSectorComparison(stockCode);

  const currency = stock.currency ?? getCurrencyFromMarket(stock.market);
  const isUsd = currency === 'USD';
  const priceUnit = isUsd ? '' : '원';

  if (fundLoading) {
    return <CompanyAnalysisSkeleton />;
  }

  return (
    <Card className="rounded-xl border-0 bg-white shadow-none">
      <CardContent className="p-4">

        <div className="mb-1 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <BarChart3 className="h-4 w-4 text-[#3182F6]" />
            <h2 className="text-sm font-semibold text-gray-900">
              기업 분석
            </h2>
            <span className="rounded-full bg-blue-50 px-2 py-0.5 text-[10px] font-medium text-[#3182F6]">
              AI
            </span>
          </div>
          {fundamentals?.bsnsYear && (
            <span className="text-[10px] text-gray-400">
              {fundamentals.bsnsYear}년 기준
            </span>
          )}
        </div>

        <SectionHeader title="밸류에이션" />
        <div className="divide-y divide-gray-100">
          <MetricRow
            label="시가총액"
            value={stock.marketCap}
            helpKey="시가총액"
          />

          {sectorData && sectorData.currentPer !== null && sectorData.sectorAvgPer !== null ? (
            <ComparisonBar
              label="PER"
              currentValue={sectorData.currentPer}
              avgValue={sectorData.sectorAvgPer}
              evaluation={sectorData.perEvaluation}
              helpKey="PER"
            />
          ) : (
            <MetricRow
              label="PER"
              value={stock.per ? `${stock.per}배` : '-'}
              helpKey="PER"
            />
          )}

          {sectorData && sectorData.currentPbr !== null && sectorData.sectorAvgPbr !== null ? (
            <ComparisonBar
              label="PBR"
              currentValue={sectorData.currentPbr}
              avgValue={sectorData.sectorAvgPbr}
              evaluation={sectorData.pbrEvaluation}
              helpKey="PBR"
            />
          ) : (
            <MetricRow
              label="PBR"
              value={stock.pbr ? `${stock.pbr}배` : '-'}
              helpKey="PBR"
            />
          )}

          <MetricRow
            label="EPS"
            value={stock.eps ? (isUsd ? formatPrice(stock.eps, 'USD') : `${formatPrice(stock.eps)}원`) : '-'}
            helpKey="EPS"
          />
          <MetricRow
            label="배당수익률"
            value={stock.dividendYield ? `${stock.dividendYield}%` : '-'}
            helpKey="배당수익률"
          />
          <MetricRow
            label="52주 최고"
            value={stock.high52w ? (isUsd ? formatPrice(stock.high52w, 'USD') : `${formatPrice(stock.high52w)}원`) : '-'}
            helpKey="52주 최고"
          />
          <MetricRow
            label="52주 최저"
            value={stock.low52w ? (isUsd ? formatPrice(stock.low52w, 'USD') : `${formatPrice(stock.low52w)}원`) : '-'}
            helpKey="52주 최저"
          />
        </div>

        {sectorData && (
          <div className="mt-1 flex items-center gap-1.5">
            <Building2 className="h-3 w-3 text-gray-400" />
            <span className="text-[10px] text-gray-400">
              {sectorData.sector} ({sectorData.peerCount}개 기업 비교)
            </span>
          </div>
        )}

        {fundamentals && (
          <>
            <SectionHeader title="수익성" />
            <div className="divide-y divide-gray-100">

              <MetricRow
                label="영업이익률"
                value={
                  fundamentals.operatingMargin !== null
                    ? `${fundamentals.operatingMargin > 0 ? '+' : ''}${fundamentals.operatingMargin.toFixed(1)}%`
                    : '-'
                }
                valueColorClass={
                  fundamentals.operatingMargin !== null
                    ? fundamentals.operatingMargin > 0
                      ? 'text-[#F04452]'
                      : fundamentals.operatingMargin < 0
                        ? 'text-[#3182F6]'
                        : 'text-gray-900'
                    : 'text-gray-400'
                }
              />

              <MetricRow
                label="ROE"
                value={
                  fundamentals.roe !== null
                    ? `${fundamentals.roe > 0 ? '+' : ''}${fundamentals.roe.toFixed(1)}%`
                    : '-'
                }
                valueColorClass={
                  fundamentals.roe !== null
                    ? fundamentals.roe >= 10
                      ? 'text-[#00B894]'
                      : fundamentals.roe < 5
                        ? 'text-[#F59E0B]'
                        : 'text-gray-900'
                    : 'text-gray-400'
                }
              />

              <MetricRow
                label="매출성장률"
                value={
                  fundamentals.revenueGrowthRate !== null
                    ? `${fundamentals.revenueGrowthRate > 0 ? '+' : ''}${fundamentals.revenueGrowthRate.toFixed(1)}%`
                    : '-'
                }
                valueColorClass={
                  fundamentals.revenueGrowthRate !== null
                    ? fundamentals.revenueGrowthRate > 0
                      ? 'text-[#F04452]'
                      : fundamentals.revenueGrowthRate < 0
                        ? 'text-[#3182F6]'
                        : 'text-gray-900'
                    : 'text-gray-400'
                }
                icon={
                  fundamentals.revenueGrowthRate !== null ? (
                    fundamentals.revenueGrowthRate > 0 ? (
                      <TrendingUp className="h-3.5 w-3.5 text-[#F04452]" />
                    ) : fundamentals.revenueGrowthRate < 0 ? (
                      <TrendingDown className="h-3.5 w-3.5 text-[#3182F6]" />
                    ) : undefined
                  ) : undefined
                }
              />

              <MetricRow
                label="매출액"
                value={
                  fundamentals.revenue !== null
                    ? formatLargeAmount(fundamentals.revenue)
                    : '-'
                }
              />
            </div>
          </>
        )}

        {fundamentals && (
          <>
            <SectionHeader title="안정성" />
            <div className="divide-y divide-gray-100">

              <MetricRow
                label="부채비율"
                value={
                  fundamentals.debtRatio !== null
                    ? `${fundamentals.debtRatio.toFixed(1)}%`
                    : '-'
                }
                valueColorClass={
                  fundamentals.debtRatio !== null
                    ? fundamentals.debtRatio > 200
                      ? 'text-[#F59E0B]'
                      : 'text-gray-900'
                    : 'text-gray-400'
                }
                icon={
                  fundamentals.debtRatio !== null && fundamentals.debtRatio > 200 ? (
                    <AlertTriangle className="h-3.5 w-3.5 text-[#F59E0B]" />
                  ) : undefined
                }
              />

              <MetricRow
                label="영업현금흐름"
                value={
                  fundamentals.operatingCashFlow !== null
                    ? formatLargeAmount(fundamentals.operatingCashFlow)
                    : '-'
                }
                valueColorClass={
                  fundamentals.operatingCashFlow !== null
                    ? fundamentals.operatingCashFlow > 0
                      ? 'text-[#00B894]'
                      : 'text-[#F04452]'
                    : 'text-gray-400'
                }
                icon={
                  fundamentals.operatingCashFlow !== null ? (
                    fundamentals.operatingCashFlow > 0 ? (
                      <CheckCircle className="h-3.5 w-3.5 text-[#00B894]" />
                    ) : (
                      <XCircle className="h-3.5 w-3.5 text-[#F04452]" />
                    )
                  ) : undefined
                }
              />
            </div>
          </>
        )}

        {fundamentals?.evaluationTags && fundamentals.evaluationTags.length > 0 && (
          <>
            <SectionHeader title="종합 판단" />
            <div className="pt-2">

              <div className="flex flex-wrap gap-1.5 mb-2">
                {fundamentals.evaluationTags.map((tag, idx) => {
                  const color = getTagColor(tag);
                  return (
                    <span
                      key={idx}
                      className={cn(
                        'rounded-full px-2.5 py-1 text-[11px] font-medium',
                        color.bg,
                        color.text
                      )}
                    >
                      {tag}
                    </span>
                  );
                })}
              </div>

              {fundamentals.summaryComment && (
                <p className="text-sm text-gray-700 leading-relaxed">
                  {fundamentals.summaryComment}
                </p>
              )}
            </div>
          </>
        )}

        <div className="mt-4 space-y-1 border-t border-gray-100 pt-3">
          {fundamentals?.bsnsYear && (
            <p className="text-[10px] text-gray-400">
              출처: KIS 한국투자증권 / DART 전자공시시스템 ({fundamentals.bsnsYear} 사업연도)
            </p>
          )}
          {!fundamentals && (
            <p className="text-[10px] text-gray-400">
              출처: KIS 한국투자증권{isUsd ? ' (해외주식)' : ''}
            </p>
          )}
          <p className="text-[10px] text-gray-400">
            이 분석은 교육 목적이며 투자 권유가 아닙니다
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
