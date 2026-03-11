'use client';

import { useState } from 'react';
import { BrainCircuit } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { usePrediction } from '@/hooks/use-analysis';
import { cn } from '@/lib/utils';

type PredictionPeriod = '1D' | '1W';

const PERIOD_TABS: { value: PredictionPeriod; label: string }[] = [
  { value: '1D', label: '1거래일' },
  { value: '1W', label: '1주일' },
];

function PredictionSkeleton() {
  return (
    <Card className="rounded-xl border-0 bg-white shadow-none">
      <CardContent className="p-4">
        <div className="mb-3 flex items-center gap-2">
          <Skeleton className="h-4 w-4 rounded" />
          <Skeleton className="h-4 w-28" />
          <Skeleton className="h-4 w-8 rounded-full" />
        </div>
        <div className="mb-4 flex gap-2">
          <Skeleton className="h-8 w-20 rounded-lg" />
          <Skeleton className="h-8 w-20 rounded-lg" />
        </div>
        <div className="space-y-2">
          <Skeleton className="h-3 w-full" />
          <Skeleton className="h-3 w-5/6" />
          <Skeleton className="h-3 w-4/6" />
        </div>
      </CardContent>
    </Card>
  );
}

interface PredictionCardProps {

  stockCode: string;
}

export function PredictionCard({ stockCode }: PredictionCardProps) {
  const [period, setPeriod] = useState<PredictionPeriod>('1D');
  const { data: prediction, isLoading } = usePrediction(stockCode, period);

  if (isLoading) {
    return <PredictionSkeleton />;
  }

  if (!prediction) {
    return (
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <div className="mb-3 flex items-center gap-2">
            <BrainCircuit className="h-4 w-4 text-[#3182F6]" />
            <h2 className="text-[15px] font-bold text-gray-900">
              AI 기반 분석
            </h2>
            <span className="rounded-full bg-blue-50 px-2.5 py-0.5 text-[11px] font-semibold text-[#3182F6]">
              AI
            </span>
          </div>
          <p className="py-4 text-center text-sm text-gray-400">
            아직 분석 데이터가 준비되지 않았습니다
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="rounded-xl border-0 bg-white shadow-none">
      <CardContent className="p-4">

        <div className="mb-3 flex items-center gap-2">
          <BrainCircuit className="h-4 w-4 text-[#3182F6]" />
          <h2 className="text-sm font-semibold text-gray-900">
            AI 기반 분석
          </h2>
          <span className="rounded-full bg-blue-50 px-2 py-0.5 text-[10px] font-medium text-[#3182F6]">
            AI
          </span>
        </div>

        <div className="mb-4 flex gap-2" role="tablist" aria-label="분석 기간 선택">
          {PERIOD_TABS.map((tab) => (
            <button
              key={tab.value}
              role="tab"
              aria-selected={period === tab.value}
              aria-label={`${tab.label} 분석 보기`}
              onClick={() => setPeriod(tab.value)}
              className={cn(
                'rounded-lg px-3.5 py-1.5 text-[13px] font-semibold transition-colors',
                period === tab.value
                  ? 'bg-gray-900 text-white'
                  : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="mb-3">
          <p className="mb-2 text-[13px] font-bold text-gray-700">
            분석 근거
          </p>
          <ul className="space-y-2">
            {prediction.reasons.map((reason, idx) => (
              <li
                key={idx}
                className="flex items-start gap-2.5 text-sm text-gray-700"
              >
                <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-[#3182F6]" />
                <span className="leading-relaxed">{reason}</span>
              </li>
            ))}
          </ul>
        </div>

        <p className="text-[11px] text-gray-400 leading-relaxed">
          {prediction.disclaimer}
        </p>
        <p className="mt-1 text-[11px] text-gray-400">
          분석 시각:{' '}
          {new Date(prediction.generatedAt).toLocaleString('ko-KR')}
        </p>
      </CardContent>
    </Card>
  );
}
