'use client';

import { useState, useMemo } from 'react';
import {
  AlertTriangle,
  ExternalLink,
  Globe,
  Shield,
  ShieldAlert,
  ShieldCheck,
  Activity,
  ChevronLeft,
  ChevronRight,
  List,
  CalendarDays,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import { formatRelativeTime } from '@/lib/format';
import {
  useGlobalEvents,
  useGlobalEventAlerts,
  useMarketRisk,
} from '@/hooks/use-global-events';
import type { EventType, GlobalEvent } from '@/types';

type EventFilter = 'ALL' | EventType;
type ViewMode = 'list' | 'calendar';

const EVENT_FILTER_TABS: { value: EventFilter; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'GEOPOLITICAL', label: '지정학' },
  { value: 'FISCAL', label: '재정/정치' },
  { value: 'INDUSTRY', label: '산업' },
  { value: 'BLACK_SWAN', label: '블랙스완' },
];

const RISK_COLORS: Record<string, { text: string; bg: string; border: string; dot: string; bar: string }> = {
  EXTREME: { text: 'text-red-600', bg: 'bg-red-50', border: 'border-red-200', dot: 'bg-red-500', bar: 'bg-red-500' },
  CRITICAL: { text: 'text-red-600', bg: 'bg-red-50', border: 'border-red-200', dot: 'bg-red-500', bar: 'bg-red-500' },
  HIGH: { text: 'text-orange-500', bg: 'bg-orange-50', border: 'border-orange-200', dot: 'bg-orange-400', bar: 'bg-orange-400' },
  MODERATE: { text: 'text-yellow-600', bg: 'bg-yellow-50', border: 'border-yellow-200', dot: 'bg-yellow-400', bar: 'bg-yellow-400' },
  MEDIUM: { text: 'text-yellow-600', bg: 'bg-yellow-50', border: 'border-yellow-200', dot: 'bg-yellow-400', bar: 'bg-yellow-400' },
  LOW: { text: 'text-green-600', bg: 'bg-green-50', border: 'border-green-200', dot: 'bg-green-400', bar: 'bg-green-400' },
};

const DEFAULT_RISK_COLOR = { text: 'text-gray-600', bg: 'bg-gray-50', border: 'border-gray-200', dot: 'bg-gray-400', bar: 'bg-gray-400' };

function getRiskColor(level: string) {
  return RISK_COLORS[level] ?? DEFAULT_RISK_COLOR;
}

const RISK_LABELS: Record<string, string> = {
  EXTREME: '심각', CRITICAL: '심각', HIGH: '높음', MODERATE: '보통', MEDIUM: '보통', LOW: '낮음',
};

const EVENT_TYPE_LABELS: Record<string, string> = {
  GEOPOLITICAL: '지정학',
  FISCAL: '재정/정치',
  INDUSTRY: '산업',
  BLACK_SWAN: '블랙스완',
};

const EVENT_TYPE_COLORS: Record<string, string> = {
  GEOPOLITICAL: 'bg-purple-50 text-purple-600 border-purple-200',
  FISCAL: 'bg-blue-50 text-blue-600 border-blue-200',
  INDUSTRY: 'bg-teal-50 text-teal-600 border-teal-200',
  BLACK_SWAN: 'bg-red-50 text-red-700 border-red-200',
};

const RISK_ACCENT_COLORS: Record<string, string> = {
  EXTREME: 'bg-red-500', CRITICAL: 'bg-red-500', HIGH: 'bg-orange-400',
  MODERATE: 'bg-yellow-400', MEDIUM: 'bg-yellow-400', LOW: 'bg-green-400',
};

const YIELD_CURVE_LABELS: Record<string, { label: string; color: string }> = {
  NORMAL: { label: '정상', color: 'text-green-600' },
  FLAT: { label: '평탄', color: 'text-yellow-600' },
  INVERTED: { label: '역전', color: 'text-red-600' },
};

const DAY_LABELS = ['일', '월', '화', '수', '목', '금', '토'];

function riskScoreToPercent(score: number): number {
  return Math.min(Math.max(score, 0), 100);
}

/* ── Skeleton components ── */

function RiskSummarySkeleton() {
  return (
    <Card className="rounded-2xl border-0 bg-white shadow-none">
      <CardContent className="p-5 space-y-4">
        <div className="flex items-center gap-2">
          <Skeleton className="h-5 w-5 rounded" />
          <Skeleton className="h-5 w-40" />
        </div>
        <Skeleton className="h-3 w-full rounded-full" />
        <Skeleton className="h-4 w-3/4" />
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="rounded-xl bg-gray-50 p-4 space-y-2">
              <Skeleton className="h-3 w-20" />
              <Skeleton className="h-6 w-16" />
              <Skeleton className="h-3 w-24" />
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

function EventListSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 4 }).map((_, i) => (
        <div key={i} className="flex rounded-2xl bg-white overflow-hidden">
          <Skeleton className="w-1 shrink-0" />
          <div className="flex-1 p-4 space-y-2">
            <div className="flex gap-2">
              <Skeleton className="h-5 w-12 rounded-full" />
              <Skeleton className="h-5 w-14 rounded-full" />
            </div>
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-3 w-1/2" />
          </div>
        </div>
      ))}
    </div>
  );
}

function RiskShieldIcon({ level, className }: { level: string; className?: string }) {
  const colors = getRiskColor(level);
  if (level === 'EXTREME' || level === 'CRITICAL' || level === 'HIGH') {
    return <ShieldAlert className={cn('h-4 w-4', colors.text, className)} />;
  }
  if (level === 'MODERATE' || level === 'MEDIUM') {
    return <Shield className={cn('h-4 w-4', colors.text, className)} />;
  }
  return <ShieldCheck className={cn('h-4 w-4', colors.text, className)} />;
}

/* ── Calendar utilities ── */

function getMonthDays(year: number, month: number) {
  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const daysInPrevMonth = new Date(year, month, 0).getDate();

  const cells: { day: number; isCurrentMonth: boolean; date: string }[] = [];

  /* Previous month trailing days */
  for (let i = firstDay - 1; i >= 0; i--) {
    const d = daysInPrevMonth - i;
    const prevMonth = month === 0 ? 11 : month - 1;
    const prevYear = month === 0 ? year - 1 : year;
    cells.push({
      day: d,
      isCurrentMonth: false,
      date: `${prevYear}-${String(prevMonth + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`,
    });
  }

  /* Current month days */
  for (let d = 1; d <= daysInMonth; d++) {
    cells.push({
      day: d,
      isCurrentMonth: true,
      date: `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`,
    });
  }

  /* Next month leading days */
  const remaining = 42 - cells.length;
  for (let d = 1; d <= remaining; d++) {
    const nextMonth = month === 11 ? 0 : month + 1;
    const nextYear = month === 11 ? year + 1 : year;
    cells.push({
      day: d,
      isCurrentMonth: false,
      date: `${nextYear}-${String(nextMonth + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`,
    });
  }

  return cells;
}

function toDateKey(dateStr: string): string {
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/* ── Calendar View ── */

function CalendarView({ events }: { events: GlobalEvent[] }) {
  const today = new Date();
  const [viewYear, setViewYear] = useState(today.getFullYear());
  const [viewMonth, setViewMonth] = useState(today.getMonth());
  const [selectedDate, setSelectedDate] = useState<string | null>(null);

  const cells = useMemo(() => getMonthDays(viewYear, viewMonth), [viewYear, viewMonth]);

  /* Group events by date */
  const eventsByDate = useMemo(() => {
    const map = new Map<string, GlobalEvent[]>();
    events.forEach((e) => {
      const key = toDateKey(e.publishedAt);
      const existing = map.get(key) ?? [];
      existing.push(e);
      map.set(key, existing);
    });
    return map;
  }, [events]);

  const selectedEvents = selectedDate ? eventsByDate.get(selectedDate) ?? [] : [];

  const handlePrevMonth = () => {
    if (viewMonth === 0) {
      setViewYear((y) => y - 1);
      setViewMonth(11);
    } else {
      setViewMonth((m) => m - 1);
    }
    setSelectedDate(null);
  };

  const handleNextMonth = () => {
    if (viewMonth === 11) {
      setViewYear((y) => y + 1);
      setViewMonth(0);
    } else {
      setViewMonth((m) => m + 1);
    }
    setSelectedDate(null);
  };

  const todayKey = toDateKey(today.toISOString());

  return (
    <div className="space-y-3">
      <Card className="rounded-2xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          {/* Month navigation */}
          <div className="flex items-center justify-between mb-4">
            <button
              onClick={handlePrevMonth}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-gray-500 hover:bg-gray-100 transition-colors"
              aria-label="이전 달"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span className="text-[15px] font-bold text-gray-900">
              {viewYear}년 {viewMonth + 1}월
            </span>
            <button
              onClick={handleNextMonth}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-gray-500 hover:bg-gray-100 transition-colors"
              aria-label="다음 달"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>

          {/* Day-of-week header */}
          <div className="grid grid-cols-7 mb-1">
            {DAY_LABELS.map((label) => (
              <div
                key={label}
                className="text-center text-[11px] font-semibold text-gray-400 py-1"
              >
                {label}
              </div>
            ))}
          </div>

          {/* Calendar grid */}
          <div className="grid grid-cols-7">
            {cells.map((cell, idx) => {
              const cellEvents = eventsByDate.get(cell.date) ?? [];
              const hasEvents = cellEvents.length > 0;
              const isToday = cell.date === todayKey;
              const isSelected = cell.date === selectedDate;

              /* Determine highest risk level for dot color */
              let dotColor = 'bg-[#3182F6]';
              if (hasEvents) {
                const hasHigh = cellEvents.some((e) =>
                  ['EXTREME', 'CRITICAL', 'HIGH'].includes(e.riskLevel)
                );
                const hasMedium = cellEvents.some((e) =>
                  ['MODERATE', 'MEDIUM'].includes(e.riskLevel)
                );
                if (hasHigh) dotColor = 'bg-red-500';
                else if (hasMedium) dotColor = 'bg-yellow-500';
              }

              return (
                <button
                  key={idx}
                  onClick={() => setSelectedDate(cell.date)}
                  className={cn(
                    'relative flex flex-col items-center py-2 rounded-lg transition-colors',
                    cell.isCurrentMonth ? 'text-gray-900' : 'text-gray-300',
                    isSelected
                      ? 'bg-[#3182F6] text-white'
                      : isToday
                        ? 'bg-blue-50'
                        : 'hover:bg-gray-50'
                  )}
                >
                  <span className={cn(
                    'text-[13px] tabular-nums',
                    isToday && !isSelected && 'font-bold text-[#3182F6]',
                    isSelected && 'font-bold'
                  )}>
                    {cell.day}
                  </span>
                  {hasEvents && cell.isCurrentMonth && (
                    <div className="flex gap-0.5 mt-0.5">
                      <span
                        className={cn(
                          'h-1 w-1 rounded-full',
                          isSelected ? 'bg-white' : dotColor
                        )}
                      />
                      {cellEvents.length > 1 && (
                        <span
                          className={cn(
                            'h-1 w-1 rounded-full',
                            isSelected ? 'bg-white/60' : 'bg-gray-300'
                          )}
                        />
                      )}
                    </div>
                  )}
                </button>
              );
            })}
          </div>
        </CardContent>
      </Card>

      {/* Selected date events */}
      {selectedDate && (
        <div className="space-y-2">
          <p className="text-[13px] font-semibold text-gray-700 px-1">
            {formatCalendarDate(selectedDate)} 이벤트
            {selectedEvents.length > 0 && (
              <span className="ml-1 text-gray-400 font-normal">
                {selectedEvents.length}건
              </span>
            )}
          </p>

          {selectedEvents.length > 0 ? (
            selectedEvents.map((event) => {
              const riskColors = getRiskColor(event.riskLevel);
              const accentColor = RISK_ACCENT_COLORS[event.riskLevel] ?? 'bg-gray-300';
              return (
                <Card key={event.id} className="rounded-xl border-0 bg-white shadow-none overflow-hidden">
                  <div className="flex items-stretch">
                    <div className={cn('w-1 shrink-0', accentColor)} />
                    <CardContent className="flex-1 p-3 space-y-1.5">
                      <div className="flex items-center gap-2 flex-wrap">
                        <Badge
                          variant="outline"
                          className={cn('text-[10px] px-1.5 py-0 font-semibold', riskColors.bg, riskColors.text, riskColors.border)}
                        >
                          {RISK_LABELS[event.riskLevel] ?? event.riskLevel}
                        </Badge>
                        <Badge
                          variant="outline"
                          className={cn('text-[10px] px-1.5 py-0', EVENT_TYPE_COLORS[event.eventType] ?? '')}
                        >
                          {EVENT_TYPE_LABELS[event.eventType] ?? event.eventType}
                        </Badge>
                      </div>
                      <h3 className="text-[13px] font-bold text-gray-900 leading-snug line-clamp-2">
                        {event.title}
                      </h3>
                      <div className="flex items-center justify-between">
                        <span className="text-[11px] text-gray-400">
                          {event.sourceName}
                        </span>
                        {event.sourceUrl && (
                          <a
                            href={event.sourceUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="flex items-center gap-1 text-[11px] text-[#3182F6] font-semibold hover:underline"
                          >
                            <ExternalLink className="h-3 w-3" />
                            원문
                          </a>
                        )}
                      </div>
                    </CardContent>
                  </div>
                </Card>
              );
            })
          ) : (
            <div className="py-8 text-center text-[13px] text-gray-400">
              해당 날짜에 이벤트가 없습니다
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function formatCalendarDate(dateStr: string): string {
  const [y, m, d] = dateStr.split('-');
  return `${Number(m)}월 ${Number(d)}일`;
}

/* ── Main Page Component ── */

export default function GlobalEventsPage() {
  const [eventFilter, setEventFilter] = useState<EventFilter>('ALL');
  const [viewMode, setViewMode] = useState<ViewMode>('list');

  const { data: riskData, isLoading: isRiskLoading } = useMarketRisk();
  const { data: alertsData } = useGlobalEventAlerts();
  const { data: eventsData, isLoading: isEventsLoading } = useGlobalEvents(
    eventFilter === 'ALL' ? undefined : eventFilter
  );

  const alerts = alertsData?.events ?? [];
  const events = eventsData?.events ?? [];

  return (
    <div className="mx-auto max-w-screen-xl p-4 pb-8 md:p-6 md:pb-10 space-y-5">

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Globe className="h-5 w-5 text-[#3182F6]" />
          <h1 className="text-2xl font-bold text-gray-900">글로벌 이벤트</h1>
        </div>

        {/* Feature 8: List/Calendar view toggle */}
        <div className="flex gap-1 rounded-lg bg-gray-100 p-0.5">
          <button
            onClick={() => setViewMode('list')}
            className={cn(
              'flex h-8 w-8 items-center justify-center rounded-md transition-all',
              viewMode === 'list'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-400 hover:text-gray-600'
            )}
            aria-label="리스트 보기"
          >
            <List className="h-4 w-4" />
          </button>
          <button
            onClick={() => setViewMode('calendar')}
            className={cn(
              'flex h-8 w-8 items-center justify-center rounded-md transition-all',
              viewMode === 'calendar'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-400 hover:text-gray-600'
            )}
            aria-label="캘린더 보기"
          >
            <CalendarDays className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Risk summary card (shared between views) */}
      {isRiskLoading ? (
        <RiskSummarySkeleton />
      ) : riskData ? (
        <Card className="rounded-2xl border-0 bg-white shadow-none overflow-hidden">
          <CardContent className="p-5 space-y-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Activity className={cn('h-5 w-5', getRiskColor(riskData.overallRiskLevel).text)} />
                <span className="text-[15px] font-bold text-gray-900">시장 리스크 종합</span>
                <Badge
                  variant="outline"
                  className={cn(
                    'text-[10px] px-2 py-0 font-semibold',
                    getRiskColor(riskData.overallRiskLevel).bg,
                    getRiskColor(riskData.overallRiskLevel).text,
                    getRiskColor(riskData.overallRiskLevel).border
                  )}
                >
                  {RISK_LABELS[riskData.overallRiskLevel] ?? riskData.overallRiskLevel}
                </Badge>
              </div>
              {riskData.yieldCurveStatus && (
                <span className="text-xs text-gray-500">
                  수익률곡선:{' '}
                  <span
                    className={cn(
                      'font-semibold',
                      YIELD_CURVE_LABELS[riskData.yieldCurveStatus]?.color ?? 'text-gray-600'
                    )}
                  >
                    {YIELD_CURVE_LABELS[riskData.yieldCurveStatus]?.label ?? riskData.yieldCurveStatus}
                  </span>
                </span>
              )}
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-gray-500">리스크 점수</span>
                <span className={cn(
                  'text-base font-bold tabular-nums',
                  getRiskColor(riskData.overallRiskLevel).text
                )}>
                  {riskData.overallRiskScore}
                  <span className="text-xs font-normal text-gray-400">/100</span>
                </span>
              </div>
              <div className="h-2.5 w-full rounded-full bg-gray-100 overflow-hidden">
                <div
                  className="h-full rounded-full transition-all duration-700 gradient-bar-risk"
                  style={{ width: `${riskScoreToPercent(riskData.overallRiskScore)}%` }}
                />
              </div>
              {riskData.riskComment && (
                <p className="text-xs text-gray-500 leading-relaxed">
                  {riskData.riskComment}
                </p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
              {(riskData.indicators ?? []).map((indicator) => {
                const colors = getRiskColor(indicator.riskLevel);
                return (
                  <div
                    key={indicator.code}
                    className="rounded-xl bg-gray-50/80 p-3.5 space-y-2 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_4px_12px_rgba(0,0,0,0.05)] cursor-default"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-semibold text-gray-500 truncate">
                        {indicator.name}
                      </span>
                      <RiskShieldIcon level={indicator.riskLevel} className="h-3.5 w-3.5" />
                    </div>
                    <p className={cn('text-base font-bold tabular-nums', colors.text)}>
                      {indicator.value.toLocaleString('ko-KR', { maximumFractionDigits: 2 })}
                      {indicator.unit && (
                        <span className="text-[11px] text-gray-400 ml-0.5 font-normal">{indicator.unit}</span>
                      )}
                    </p>
                    <div className="h-1 w-full rounded-full bg-gray-200 overflow-hidden">
                      <div
                        className={cn('h-full rounded-full', colors.bar)}
                        style={{
                          width: indicator.riskLevel === 'EXTREME' || indicator.riskLevel === 'CRITICAL' ? '100%'
                            : indicator.riskLevel === 'HIGH' ? '75%'
                            : indicator.riskLevel === 'MODERATE' || indicator.riskLevel === 'MEDIUM' ? '50%'
                            : '25%'
                        }}
                      />
                    </div>
                    {indicator.changeRate != null && (
                      <p className={cn(
                        'text-[11px] font-semibold tabular-nums',
                        indicator.changeRate > 0 ? 'text-up' : indicator.changeRate < 0 ? 'text-down' : 'text-flat'
                      )}>
                        {indicator.changeRate > 0 ? '+' : ''}{indicator.changeRate.toFixed(2)}%
                      </p>
                    )}
                    <p className="text-[10px] text-gray-500 leading-tight line-clamp-2">
                      {indicator.description}
                    </p>
                  </div>
                );
              })}
            </div>
          </CardContent>
        </Card>
      ) : null}

      {/* Alerts banner */}
      {alerts.length > 0 && (
        <Card className="rounded-2xl border border-red-200 animate-subtle-pulse bg-red-50 shadow-none overflow-hidden">
          <CardContent className="p-0">
            <div className="flex items-stretch">
              <div className="w-1.5 bg-red-500 shrink-0" />
              <div className="flex-1 p-4 md:p-5">
                <div className="flex items-center gap-2 mb-3">
                  <div className="flex h-7 w-7 items-center justify-center rounded-full bg-red-100">
                    <AlertTriangle className="h-3.5 w-3.5 text-red-600" />
                  </div>
                  <span className="text-sm font-bold text-red-700">24시간 내 고위험 이벤트</span>
                  <Badge
                    variant="outline"
                    className="text-[10px] px-1.5 py-0 bg-red-100 text-red-700 border-red-300 font-semibold"
                  >
                    {alerts.length}건
                  </Badge>
                </div>
                <ul className="space-y-2">
                  {alerts.slice(0, 3).map((alert) => (
                    <li key={alert.id} className="flex items-start gap-2">
                      <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-red-500" />
                      <span className="text-sm text-red-800 leading-snug line-clamp-1 font-semibold">
                        {alert.title}
                      </span>
                    </li>
                  ))}
                  {alerts.length > 3 && (
                    <li className="text-[11px] text-red-500 font-medium pl-4">
                      외 {alerts.length - 3}건 더
                    </li>
                  )}
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Event filter tabs */}
      <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-1">
        {EVENT_FILTER_TABS.map((tab) => (
          <button
            key={tab.value}
            className={cn(
              'shrink-0 rounded-full px-4 py-2 text-[13px] font-semibold transition-colors',
              eventFilter === tab.value
                ? 'bg-gray-900 text-white'
                : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
            )}
            onClick={() => setEventFilter(tab.value)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content based on view mode */}
      {viewMode === 'list' ? (
        /* ── List view ── */
        isEventsLoading ? (
          <EventListSkeleton />
        ) : events.length > 0 ? (
          <div className="space-y-3">
            {events.map((event) => {
              const riskColors = getRiskColor(event.riskLevel);
              const accentColor = RISK_ACCENT_COLORS[event.riskLevel] ?? 'bg-gray-300';
              return (
                <Card key={event.id} className="group rounded-2xl border-0 bg-white shadow-none overflow-hidden transition-all duration-200 hover:shadow-[0_2px_8px_rgba(0,0,0,0.04)]">
                  <div className="flex items-stretch">
                    <div className={cn('w-1 group-hover:w-1.5 shrink-0 transition-all duration-200', accentColor)} />
                    <CardContent className="flex-1 p-4 md:p-5 space-y-2.5">
                      <div className="flex items-center gap-2 flex-wrap">
                        <Badge
                          variant="outline"
                          className={cn('text-[10px] px-1.5 py-0 font-semibold', riskColors.bg, riskColors.text, riskColors.border)}
                        >
                          {RISK_LABELS[event.riskLevel] ?? event.riskLevel}
                        </Badge>
                        <Badge
                          variant="outline"
                          className={cn('text-[10px] px-1.5 py-0', EVENT_TYPE_COLORS[event.eventType] ?? '')}
                        >
                          {EVENT_TYPE_LABELS[event.eventType] ?? event.eventType}
                        </Badge>
                      </div>
                      <h3 className="text-sm font-bold text-gray-900 leading-snug line-clamp-2">
                        {event.title}
                      </h3>
                      {event.summary && (
                        <p className="text-[13px] text-gray-600 leading-relaxed line-clamp-3">
                          {event.summary}
                        </p>
                      )}
                      <div className="flex items-center justify-between pt-1">
                        <div className="flex items-center gap-2">
                          <span className="text-[11px] text-gray-500">{event.sourceName}</span>
                          <span className="text-[11px] text-gray-400">
                            {formatRelativeTime(event.publishedAt)}
                          </span>
                        </div>
                        {event.sourceUrl && (
                          <a
                            href={event.sourceUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="flex items-center gap-1 text-[11px] text-[#3182F6] font-semibold hover:underline"
                          >
                            <ExternalLink className="h-3 w-3" />
                            원문
                          </a>
                        )}
                      </div>
                    </CardContent>
                  </div>
                </Card>
              );
            })}
          </div>
        ) : (
          <div className="py-16 text-center text-[13px] text-gray-400">
            해당 유형의 이벤트가 없습니다
          </div>
        )
      ) : (
        /* ── Calendar view ── */
        isEventsLoading ? (
          <EventListSkeleton />
        ) : (
          <CalendarView events={events} />
        )
      )}
    </div>
  );
}
