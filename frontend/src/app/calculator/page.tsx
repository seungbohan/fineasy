/**
 * @file ETF compound interest calculator page
 * @description Calculates future portfolio value with monthly contributions using compound interest.
 *   Supports ETF presets (QQQ, SPY, SCHD, etc.) and custom annual return rates.
 *   Displays results as summary cards, an area chart (principal vs growth), and a yearly table.
 */
'use client';

import { useState, useMemo, useCallback } from 'react';
import {
  Calculator,
  TrendingUp,
  DollarSign,
  Percent,
  Calendar,
  ChevronDown,
  ChevronUp,
  Info,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';

/* ------------------------------------------------------------------ */
/*  ETF presets                                                        */
/* ------------------------------------------------------------------ */

interface EtfPreset {
  name: string;
  ticker: string;
  annualReturn: number;
  description: string;
}

const ETF_PRESETS: EtfPreset[] = [
  { name: 'QQQ', ticker: 'QQQ', annualReturn: 15.2, description: 'NASDAQ-100 추종' },
  { name: 'SPY', ticker: 'SPY', annualReturn: 10.5, description: 'S&P 500 추종' },
  { name: 'SCHD', ticker: 'SCHD', annualReturn: 12.1, description: '미국 배당성장' },
  { name: 'VTI', ticker: 'VTI', annualReturn: 10.8, description: '미국 전체 시장' },
  { name: 'VOO', ticker: 'VOO', annualReturn: 10.6, description: 'S&P 500 추종' },
  { name: 'TIGER S&P500', ticker: 'TIGER', annualReturn: 10.3, description: '국내 상장 S&P500' },
];

/* ------------------------------------------------------------------ */
/*  Calculation types                                                  */
/* ------------------------------------------------------------------ */

interface YearlyRow {
  year: number;
  principal: number;
  totalValue: number;
  growth: number;
}

/* ------------------------------------------------------------------ */
/*  Formatting helpers                                                 */
/* ------------------------------------------------------------------ */

function formatKRW(value: number): string {
  if (value >= 1_0000_0000) {
    return `${(value / 1_0000_0000).toFixed(1)}억원`;
  }
  if (value >= 1_0000) {
    return `${(value / 1_0000).toFixed(0)}만원`;
  }
  return `${value.toLocaleString()}원`;
}

function formatFullKRW(value: number): string {
  return `${Math.round(value).toLocaleString()}원`;
}

/* ------------------------------------------------------------------ */
/*  Custom chart tooltip                                               */
/* ------------------------------------------------------------------ */

function CustomTooltip({
  active,
  payload,
  label,
}: {
  active?: boolean;
  payload?: Array<{ value: number; name: string; color: string }>;
  label?: string;
}) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg border border-gray-100 bg-white p-3 text-xs shadow-md">
      <p className="mb-1.5 font-semibold text-gray-700">{label}년차</p>
      {payload.map((entry) => (
        <p key={entry.name} className="flex items-center gap-1.5" style={{ color: entry.color }}>
          <span
            className="inline-block h-2 w-2 rounded-full"
            style={{ backgroundColor: entry.color }}
          />
          {entry.name}: {formatKRW(entry.value)}
        </p>
      ))}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Main page component                                                */
/* ------------------------------------------------------------------ */

export default function CalculatorPage() {
  /* ---- Input state ---- */
  const [initialInvestment, setInitialInvestment] = useState<string>('1000');
  const [monthlyContribution, setMonthlyContribution] = useState<string>('100');
  const [years, setYears] = useState<string>('20');
  const [annualReturn, setAnnualReturn] = useState<string>('10.5');
  const [selectedPreset, setSelectedPreset] = useState<string | null>('SPY');

  /* ---- UI state ---- */
  const [showFullTable, setShowFullTable] = useState(false);

  /* ---- Preset selection ---- */
  const handlePresetSelect = useCallback((preset: EtfPreset) => {
    setSelectedPreset(preset.ticker);
    setAnnualReturn(String(preset.annualReturn));
  }, []);

  /* ---- Calculation (monthly compound) ---- */
  const result = useMemo(() => {
    const principal0 = (parseFloat(initialInvestment) || 0) * 10000; // convert 만원 -> 원
    const monthly = (parseFloat(monthlyContribution) || 0) * 10000;
    const n = parseInt(years) || 0;
    const annualRate = (parseFloat(annualReturn) || 0) / 100;
    const monthlyRate = annualRate / 12;

    if (n <= 0) return { rows: [] as YearlyRow[], totalPrincipal: 0, totalValue: 0 };

    const rows: YearlyRow[] = [];
    let currentValue = principal0;

    for (let y = 1; y <= n; y++) {
      for (let m = 0; m < 12; m++) {
        currentValue = currentValue * (1 + monthlyRate) + monthly;
      }
      const cumulativePrincipal = principal0 + monthly * 12 * y;
      rows.push({
        year: y,
        principal: Math.round(cumulativePrincipal),
        totalValue: Math.round(currentValue),
        growth: Math.round(currentValue - cumulativePrincipal),
      });
    }

    const last = rows[rows.length - 1];
    return {
      rows,
      totalPrincipal: last?.principal ?? 0,
      totalValue: last?.totalValue ?? 0,
    };
  }, [initialInvestment, monthlyContribution, years, annualReturn]);

  const totalProfit = result.totalValue - result.totalPrincipal;
  const profitRate = result.totalPrincipal > 0 ? (totalProfit / result.totalPrincipal) * 100 : 0;

  /* ---- Chart data ---- */
  const chartData = useMemo(
    () =>
      result.rows.map((r) => ({
        name: `${r.year}`,
        투자원금: r.principal,
        총자산: r.totalValue,
      })),
    [result.rows],
  );

  /* ---- Visible table rows ---- */
  const visibleRows = showFullTable ? result.rows : result.rows.slice(0, 5);

  return (
    <div className="mx-auto max-w-screen-xl space-y-4 p-4 md:p-6">
      {/* Page header */}
      <div>
        <h1 className="text-xl font-bold text-gray-900">ETF 복리 수익률 계산기</h1>
        <p className="mt-1 text-sm text-gray-500">
          월 적립 투자 시 복리 효과로 자산이 어떻게 성장하는지 확인해보세요.
        </p>
      </div>

      {/* ETF presets */}
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <div className="mb-2.5 flex items-center gap-2">
            <TrendingUp className="h-4 w-4 text-[#3182F6]" />
            <span className="text-sm font-semibold text-gray-900">ETF 프리셋</span>
          </div>
          <div className="grid grid-cols-3 gap-2">
            {ETF_PRESETS.map((preset) => (
              <button
                key={preset.ticker}
                type="button"
                onClick={() => handlePresetSelect(preset)}
                className={`rounded-lg px-2 py-2.5 text-center transition-colors ${
                  selectedPreset === preset.ticker
                    ? 'bg-[#3182F6] text-white'
                    : 'bg-gray-50 text-gray-700 hover:bg-gray-100'
                }`}
              >
                <p className="text-xs font-semibold">{preset.name}</p>
                <p
                  className={`text-[10px] ${
                    selectedPreset === preset.ticker ? 'text-blue-100' : 'text-gray-400'
                  }`}
                >
                  연 {preset.annualReturn}%
                </p>
              </button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Input form */}
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="space-y-4 p-4">
          <div className="flex items-center gap-2">
            <Calculator className="h-4 w-4 text-[#3182F6]" />
            <span className="text-sm font-semibold text-gray-900">투자 조건 설정</span>
          </div>

          {/* Initial investment */}
          <div>
            <label htmlFor="initial" className="mb-1 block text-xs font-medium text-gray-600">
              <DollarSign className="mr-1 inline h-3 w-3" />
              초기 투자금 (만원)
            </label>
            <Input
              id="initial"
              type="number"
              inputMode="numeric"
              min={0}
              value={initialInvestment}
              onChange={(e) => setInitialInvestment(e.target.value)}
              placeholder="예: 1000"
              className="h-10"
            />
          </div>

          {/* Monthly contribution */}
          <div>
            <label htmlFor="monthly" className="mb-1 block text-xs font-medium text-gray-600">
              <DollarSign className="mr-1 inline h-3 w-3" />
              월 적립금 (만원)
            </label>
            <Input
              id="monthly"
              type="number"
              inputMode="numeric"
              min={0}
              value={monthlyContribution}
              onChange={(e) => setMonthlyContribution(e.target.value)}
              placeholder="예: 100"
              className="h-10"
            />
          </div>

          {/* Years */}
          <div>
            <label htmlFor="years" className="mb-1 block text-xs font-medium text-gray-600">
              <Calendar className="mr-1 inline h-3 w-3" />
              투자 기간 (년)
            </label>
            <Input
              id="years"
              type="number"
              inputMode="numeric"
              min={1}
              max={50}
              value={years}
              onChange={(e) => setYears(e.target.value)}
              placeholder="예: 20"
              className="h-10"
            />
          </div>

          {/* Annual return */}
          <div>
            <label htmlFor="return" className="mb-1 block text-xs font-medium text-gray-600">
              <Percent className="mr-1 inline h-3 w-3" />
              연 수익률 (%)
            </label>
            <Input
              id="return"
              type="number"
              inputMode="decimal"
              step={0.1}
              min={0}
              max={100}
              value={annualReturn}
              onChange={(e) => {
                setAnnualReturn(e.target.value);
                setSelectedPreset(null);
              }}
              placeholder="예: 10.5"
              className="h-10"
            />
          </div>
        </CardContent>
      </Card>

      {/* Results summary */}
      {result.rows.length > 0 && (
        <>
          <Card className="rounded-xl border-0 bg-white shadow-none">
            <CardContent className="p-4">
              <div className="mb-3 flex items-center gap-2">
                <TrendingUp className="h-4 w-4 text-[#3182F6]" />
                <span className="text-sm font-semibold text-gray-900">
                  {years}년 후 예상 결과
                </span>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-lg bg-gray-50 p-3">
                  <p className="text-[10px] text-gray-400">총 투자원금</p>
                  <p className="mt-0.5 text-sm font-bold text-gray-900">
                    {formatKRW(result.totalPrincipal)}
                  </p>
                </div>
                <div className="rounded-lg bg-blue-50 p-3">
                  <p className="text-[10px] text-[#3182F6]">최종 자산</p>
                  <p className="mt-0.5 text-sm font-bold text-[#3182F6]">
                    {formatKRW(result.totalValue)}
                  </p>
                </div>
                <div className="rounded-lg bg-red-50 p-3">
                  <p className="text-[10px] text-red-500">총 수익금</p>
                  <p className="mt-0.5 text-sm font-bold text-red-500">
                    +{formatKRW(totalProfit)}
                  </p>
                </div>
                <div className="rounded-lg bg-green-50 p-3">
                  <p className="text-[10px] text-green-600">수익률</p>
                  <p className="mt-0.5 text-sm font-bold text-green-600">
                    +{profitRate.toFixed(1)}%
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Area chart */}
          <Card className="rounded-xl border-0 bg-white shadow-none">
            <CardContent className="p-4">
              <div className="mb-3 flex items-center gap-2">
                <TrendingUp className="h-4 w-4 text-[#3182F6]" />
                <span className="text-sm font-semibold text-gray-900">자산 성장 추이</span>
              </div>
              <div className="h-64 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={chartData} margin={{ top: 5, right: 5, left: -10, bottom: 0 }}>
                    <defs>
                      <linearGradient id="colorTotal" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#3182F6" stopOpacity={0.15} />
                        <stop offset="95%" stopColor="#3182F6" stopOpacity={0} />
                      </linearGradient>
                      <linearGradient id="colorPrincipal" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#94A3B8" stopOpacity={0.15} />
                        <stop offset="95%" stopColor="#94A3B8" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                    <XAxis
                      dataKey="name"
                      tick={{ fontSize: 11, fill: '#94A3B8' }}
                      tickLine={false}
                      axisLine={false}
                      tickFormatter={(v) => `${v}년`}
                    />
                    <YAxis
                      tick={{ fontSize: 11, fill: '#94A3B8' }}
                      tickLine={false}
                      axisLine={false}
                      tickFormatter={(v: number) => formatKRW(v)}
                    />
                    <Tooltip content={<CustomTooltip />} />
                    <Legend
                      iconType="circle"
                      iconSize={8}
                      wrapperStyle={{ fontSize: '11px', paddingTop: '8px' }}
                    />
                    <Area
                      type="monotone"
                      dataKey="투자원금"
                      stroke="#94A3B8"
                      strokeWidth={2}
                      fill="url(#colorPrincipal)"
                    />
                    <Area
                      type="monotone"
                      dataKey="총자산"
                      stroke="#3182F6"
                      strokeWidth={2}
                      fill="url(#colorTotal)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>

          {/* Yearly detail table */}
          <Card className="rounded-xl border-0 bg-white shadow-none">
            <CardContent className="p-4">
              <div className="mb-3 flex items-center gap-2">
                <Calendar className="h-4 w-4 text-[#3182F6]" />
                <span className="text-sm font-semibold text-gray-900">연도별 상세</span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="border-b border-gray-100 text-gray-400">
                      <th className="pb-2 text-left font-medium">연차</th>
                      <th className="pb-2 text-right font-medium">투자원금</th>
                      <th className="pb-2 text-right font-medium">총자산</th>
                      <th className="pb-2 text-right font-medium">수익금</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibleRows.map((row) => (
                      <tr key={row.year} className="border-b border-gray-50">
                        <td className="py-2 text-gray-600">{row.year}년</td>
                        <td className="py-2 text-right text-gray-600">
                          {formatFullKRW(row.principal)}
                        </td>
                        <td className="py-2 text-right font-medium text-[#3182F6]">
                          {formatFullKRW(row.totalValue)}
                        </td>
                        <td className="py-2 text-right text-red-500">
                          +{formatFullKRW(row.growth)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {result.rows.length > 5 && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="mt-2 w-full text-xs text-gray-400"
                  onClick={() => setShowFullTable((p) => !p)}
                >
                  {showFullTable ? (
                    <>
                      접기 <ChevronUp className="ml-1 h-3 w-3" />
                    </>
                  ) : (
                    <>
                      전체보기 ({result.rows.length}년) <ChevronDown className="ml-1 h-3 w-3" />
                    </>
                  )}
                </Button>
              )}
            </CardContent>
          </Card>

          {/* Disclaimer */}
          <div className="flex items-start gap-1.5 px-1">
            <Info className="mt-0.5 h-3 w-3 shrink-0 text-gray-300" />
            <p className="text-[10px] leading-relaxed text-gray-400">
              본 계산기는 단순 복리 시뮬레이션이며, 과거 수익률은 미래 수익을 보장하지 않습니다.
              실제 투자 수익은 시장 상황, 환율, 수수료, 세금 등에 따라 달라질 수 있습니다.
            </p>
          </div>
        </>
      )}
    </div>
  );
}
