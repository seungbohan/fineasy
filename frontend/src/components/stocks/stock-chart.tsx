'use client';

import { useRef, useEffect, useState, useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { useStockChart } from '@/hooks/use-stocks';
import { ChartSkeleton } from '@/components/shared/loading-skeleton';
import { cn } from '@/lib/utils';

type Period = '1D' | '1W' | '1M' | '3M' | '1Y' | 'ALL';
type ChartType = 'line' | 'candle';

const PERIOD_LABELS: { value: Period; label: string }[] = [
  { value: '1D', label: '1일' },
  { value: '1W', label: '1주' },
  { value: '1M', label: '1개월' },
  { value: '3M', label: '3개월' },
  { value: '1Y', label: '1년' },
  { value: 'ALL', label: '전체' },
];

interface StockChartProps {
  stockCode: string;
  market?: string;
}

export function StockChart({ stockCode }: StockChartProps) {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<ReturnType<typeof import('lightweight-charts').createChart> | null>(null);
  const [period, setPeriod] = useState<Period>('3M');
  const [chartType, setChartType] = useState<ChartType>('candle');
  const [showMA, setShowMA] = useState(true);

  const { data: chartData, isLoading } = useStockChart(stockCode, period);

  const candles = chartData?.candles;


  const isMinuteData = period === '1D';

  const toChartTime = useCallback(
    (dateStr: string) => {
      if (isMinuteData && dateStr.includes(' ')) {
        const isoStr = dateStr.replace(' ', 'T') + ':00Z';
        return Math.floor(new Date(isoStr).getTime() / 1000);
      }
      return dateStr;
    },
    [isMinuteData]
  );


  useEffect(() => {
    if (!chartContainerRef.current || !candles || candles.length === 0) return;

    let isCleanedUp = false;

    import('lightweight-charts').then((LWC) => {
      if (isCleanedUp || !chartContainerRef.current) return;

      if (chartRef.current) {
        chartRef.current.remove();
        chartRef.current = null;
      }

      const container = chartContainerRef.current;

      const chart = LWC.createChart(container, {
        width: container.clientWidth,
        height: 350,
        layout: {
          background: { color: '#FFFFFF' },
          textColor: '#8B95A1',
          fontSize: 11,
        },
        grid: {
          vertLines: { color: '#F2F4F6' },
          horzLines: { color: '#F2F4F6' },
        },
        crosshair: {
          mode: LWC.CrosshairMode.Normal,
        },
        rightPriceScale: {
          borderColor: '#E5E8EB',
        },
        timeScale: {
          borderColor: '#E5E8EB',
          timeVisible: isMinuteData,
          secondsVisible: false,
        },
      });

      chartRef.current = chart;


      if (chartType === 'candle') {
        const candleSeries = chart.addSeries(LWC.CandlestickSeries, {
          upColor: '#F04452',
          downColor: '#3182F6',
          borderDownColor: '#3182F6',
          borderUpColor: '#F04452',
          wickDownColor: '#3182F6',
          wickUpColor: '#F04452',
        });

        candleSeries.setData(
          candles.map((c) => ({
            time: toChartTime(c.date) as string,
            open: c.open,
            high: c.high,
            low: c.low,
            close: c.close,
          }))
        );
      } else {
        const lineSeries = chart.addSeries(LWC.LineSeries, {
          color: '#3182F6',
          lineWidth: 2,
        });

        lineSeries.setData(
          candles.map((c) => ({
            time: toChartTime(c.date) as string,
            value: c.close,
          }))
        );
      }


      if (showMA && candles.length > 5) {
        const maConfigs = [
          { period: 5, color: '#F59E0B' },
          { period: 20, color: '#F04452' },
          { period: 60, color: '#3182F6' },
        ];

        maConfigs.forEach(({ period: maPeriod, color }) => {
          if (candles.length < maPeriod) return;
          const maData: { time: string; value: number }[] = [];
          for (let i = maPeriod - 1; i < candles.length; i++) {
            let sum = 0;
            for (let j = 0; j < maPeriod; j++) {
              sum += candles[i - j].close;
            }
            maData.push({
              time: toChartTime(candles[i].date) as string,
              value: Math.round(sum / maPeriod),
            });
          }
          const maSeries = chart.addSeries(LWC.LineSeries, {
            color,
            lineWidth: 1,
          });
          maSeries.setData(maData);
        });
      }


      const volumeSeries = chart.addSeries(LWC.HistogramSeries, {
        priceFormat: { type: 'volume' },
        priceScaleId: 'volume',
      });

      chart.priceScale('volume').applyOptions({
        scaleMargins: { top: 0.8, bottom: 0 },
      });

      volumeSeries.setData(
        candles.map((c) => ({
          time: toChartTime(c.date) as string,
          value: c.volume,
          color: c.close >= c.open ? 'rgba(240,68,82,0.3)' : 'rgba(49,130,246,0.3)',
        }))
      );


      chart.timeScale().fitContent();

      const handleResize = () => {
        if (chartRef.current && container) {
          chartRef.current.applyOptions({ width: container.clientWidth });
        }
      };
      window.addEventListener('resize', handleResize);

      return () => {
        window.removeEventListener('resize', handleResize);
      };
    });

    return () => {
      isCleanedUp = true;
      if (chartRef.current) {
        chartRef.current.remove();
        chartRef.current = null;
      }
    };

  }, [candles, chartType, showMA, isMinuteData]);

  if (isLoading) return <ChartSkeleton />;

  return (
    <div className="space-y-3">

      <div className="flex items-center justify-between px-1">
        <div className="flex gap-1">
          <Button
            variant={chartType === 'line' ? 'default' : 'ghost'}
            size="xs"
            onClick={() => setChartType('line')}
            className="text-xs"
          >
            라인
          </Button>
          <Button
            variant={chartType === 'candle' ? 'default' : 'ghost'}
            size="xs"
            onClick={() => setChartType('candle')}
            className="text-xs"
          >
            캔들
          </Button>
        </div>
        <Button
          variant={showMA ? 'secondary' : 'ghost'}
          size="xs"
          onClick={() => setShowMA(!showMA)}
          className="text-xs"
        >
          이동평균선
        </Button>
      </div>

      <div ref={chartContainerRef} className="w-full" />

      <div className="flex gap-1">
        {PERIOD_LABELS.map((p) => (
          <Button
            key={p.value}
            variant={period === p.value ? 'default' : 'ghost'}
            size="xs"
            onClick={() => setPeriod(p.value)}
            className={cn(
              'flex-1 text-xs',
              period === p.value && 'bg-gray-900 text-white hover:bg-gray-800'
            )}
          >
            {p.label}
          </Button>
        ))}
      </div>

      {showMA && (
        <div className="flex items-center gap-4 text-[10px] text-gray-500 px-1">
          <span className="flex items-center gap-1">
            <span className="inline-block h-0.5 w-3 bg-[#F59E0B]" /> MA5
          </span>
          <span className="flex items-center gap-1">
            <span className="inline-block h-0.5 w-3 bg-[#F04452]" /> MA20
          </span>
          <span className="flex items-center gap-1">
            <span className="inline-block h-0.5 w-3 bg-[#3182F6]" /> MA60
          </span>
        </div>
      )}
    </div>
  );
}
