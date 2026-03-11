'use client';

import { useState } from 'react';
import Link from 'next/link';
import {
  TrendingUp,
  TrendingDown,
  BarChart3,
  Banknote,
  Flame,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { StockListSkeleton } from '@/components/shared/loading-skeleton';
import { cn } from '@/lib/utils';
import { useStockRanking } from '@/hooks/use-market';
import {
  formatPrice,
  formatChangeRate,
  formatVolume,
  formatTradingValue,
  getPriceColorClass,
} from '@/lib/format';

export default function StocksPage() {
  const [region, setRegion] = useState<'domestic' | 'overseas'>('domestic');
  const [tab, setTab] = useState<'trading_value' | 'volume' | 'gainers' | 'losers'>('trading_value');

  const rankingType = tab === 'trading_value' ? 'trading_value' : tab;
  const { data: stocks, isLoading } = useStockRanking(rankingType, 30, region);

  return (
    <div className="mx-auto max-w-screen-xl p-4 pb-8 md:p-6 md:pb-10">

      <div className="mb-4 flex items-center gap-2">
        <Flame className="h-5 w-5 text-orange-500" />
        <h1 className="text-xl font-bold text-gray-900">인기 종목</h1>
      </div>

      <div className="mb-4 flex gap-1.5">
        <button
          onClick={() => setRegion('domestic')}
          className={cn(
            'rounded-full px-4 py-2 text-[13px] font-semibold transition-colors',
            region === 'domestic'
              ? 'bg-gray-900 text-white'
              : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
          )}
        >
          국내
        </button>
        <button
          onClick={() => setRegion('overseas')}
          className={cn(
            'rounded-full px-4 py-2 text-[13px] font-semibold transition-colors',
            region === 'overseas'
              ? 'bg-gray-900 text-white'
              : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
          )}
        >
          해외
        </button>
      </div>

      <Card className="rounded-2xl border-0 bg-white shadow-none">
        <CardContent className="p-0">
          <Tabs
            value={tab}
            onValueChange={(v) => setTab(v as typeof tab)}
            className="w-full"
          >
            <div className="px-4 pt-4">
              <TabsList className="h-9 w-full bg-gray-100 rounded-lg">
                <TabsTrigger value="trading_value" className="flex-1 text-xs gap-1 rounded-md">
                  <Banknote className="h-3.5 w-3.5" />
                  거래대금
                </TabsTrigger>
                <TabsTrigger value="volume" className="flex-1 text-xs gap-1 rounded-md">
                  <BarChart3 className="h-3.5 w-3.5" />
                  거래량
                </TabsTrigger>
                <TabsTrigger value="gainers" className="flex-1 text-xs gap-1 rounded-md">
                  <TrendingUp className="h-3.5 w-3.5" />
                  상승
                </TabsTrigger>
                <TabsTrigger value="losers" className="flex-1 text-xs gap-1 rounded-md">
                  <TrendingDown className="h-3.5 w-3.5" />
                  하락
                </TabsTrigger>
              </TabsList>
            </div>

            <TabsContent value={tab} className="mt-2">
              {isLoading ? (
                <StockListSkeleton count={10} />
              ) : stocks && stocks.length === 0 ? (
                <div className="py-16 text-center text-sm text-gray-400">
                  표시할 종목이 없습니다
                </div>
              ) : (
                <div>

                  <div className="flex items-center px-4 py-2 border-b border-gray-50">
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      <span className="w-6" />
                      <span className="text-[11px] text-gray-400">종목명</span>
                    </div>
                    <span className="w-20 sm:w-24 text-right text-[11px] text-gray-400">현재가</span>
                    <span className="w-16 sm:w-20 text-right text-[11px] text-gray-400">등락률</span>
                    <span className="hidden sm:block w-24 text-right text-[11px] text-gray-400">
                      {tab === 'volume' ? '거래량' : '거래대금'}
                    </span>
                  </div>
                  {stocks?.map((stock, idx) => {
                    const cur = stock.currency ?? (region === 'overseas' ? 'USD' : 'KRW');
                    const isUsd = cur === 'USD';
                    return (
                      <Link
                        key={stock.stockCode}
                        href={`/stocks/${stock.stockCode}`}
                        className="flex items-center px-4 py-3 transition-colors hover:bg-gray-50 border-b border-gray-50 last:border-0"
                      >

                        <div className="flex items-center gap-3 flex-1 min-w-0">
                          <span className={cn(
                            'flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[11px] font-bold tabular-nums',
                            idx === 0 ? 'bg-amber-100 text-amber-700' :
                            idx === 1 ? 'bg-gray-100 text-gray-500' :
                            idx === 2 ? 'bg-orange-50 text-orange-500' :
                            'bg-transparent text-gray-300'
                          )}>
                            {idx + 1}
                          </span>
                          <div className="min-w-0">
                            <p className="text-sm font-medium text-gray-900 truncate">
                              {stock.stockName}
                            </p>
                            <p className="text-[11px] text-gray-400">
                              {stock.stockCode}
                            </p>
                          </div>
                        </div>

                        <span className="w-20 sm:w-24 text-right text-sm tabular-nums text-gray-900">
                          {isUsd
                            ? formatPrice(stock.currentPrice, 'USD')
                            : formatPrice(stock.currentPrice)
                          }
                        </span>

                        <span className={cn(
                          'w-16 sm:w-20 text-right text-sm font-medium tabular-nums',
                          getPriceColorClass(stock.changeRate)
                        )}>
                          {formatChangeRate(stock.changeRate)}
                        </span>

                        <span className="hidden sm:block w-24 text-right text-xs tabular-nums text-gray-500">
                          {tab === 'volume'
                            ? `${formatVolume(stock.volume)}주`
                            : stock.tradingValue > 0
                              ? `${formatTradingValue(stock.tradingValue, cur)}${!isUsd ? '원' : ''}`
                              : '-'
                          }
                        </span>
                      </Link>
                    );
                  })}
                </div>
              )}
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
}
