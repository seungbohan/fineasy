'use client';

import { useState } from 'react';
import { Bitcoin, ArrowUpRight, ArrowDownRight } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/components/ui/sheet';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import { useCryptoList } from '@/hooks/use-crypto';
import type { CoinData } from '@/types';

const FEATURED_SYMBOLS = ['BTC', 'ETH'];

const COIN_COLORS: Record<string, { bg: string; text: string; accent: string }> = {
  BTC: { bg: 'bg-amber-50', text: 'text-amber-600', accent: 'border-amber-200' },
  ETH: { bg: 'bg-indigo-50', text: 'text-indigo-600', accent: 'border-indigo-200' },
};

function formatUsd(value: number): string {
  if (value >= 1) {
    return `$${value.toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
  return `$${value.toLocaleString('ko-KR', { minimumFractionDigits: 4, maximumFractionDigits: 4 })}`;
}

function formatKrw(value: number): string {
  return `${Math.round(value).toLocaleString('ko-KR')}원`;
}

function formatMarketCap(value: number): string {
  if (value >= 1_000_000_000_000) return `$${(value / 1_000_000_000_000).toFixed(2)}T`;
  if (value >= 1_000_000_000) return `$${(value / 1_000_000_000).toFixed(2)}B`;
  if (value >= 1_000_000) return `$${(value / 1_000_000).toFixed(2)}M`;
  return `$${value.toLocaleString('ko-KR')}`;
}

function formatVolume(value: number): string {
  if (value >= 1_000_000_000) return `$${(value / 1_000_000_000).toFixed(2)}B`;
  if (value >= 1_000_000) return `$${(value / 1_000_000).toFixed(2)}M`;
  return `$${value.toLocaleString('ko-KR')}`;
}

function formatChange(rate: number | null): string {
  if (rate == null) return '-';
  const sign = rate > 0 ? '+' : '';
  return `${sign}${rate.toFixed(2)}%`;
}

function getChangeColor(change: number | null): string {
  if (change == null || change === 0) return 'text-flat';
  return change > 0 ? 'text-up' : 'text-down';
}

function FeaturedSkeleton() {
  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
      {Array.from({ length: 2 }).map((_, i) => (
        <div key={i} className="rounded-xl bg-white p-5 space-y-3">
          <div className="flex items-center gap-3">
            <Skeleton className="h-10 w-10 rounded-full" />
            <div className="space-y-1.5">
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-3 w-16" />
            </div>
          </div>
          <Skeleton className="h-7 w-36" />
          <Skeleton className="h-4 w-24" />
          <div className="flex gap-4">
            <Skeleton className="h-3 w-20" />
            <Skeleton className="h-3 w-20" />
          </div>
        </div>
      ))}
    </div>
  );
}

function CoinListSkeleton() {
  return (
    <div className="space-y-1">
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <Skeleton className="h-8 w-8 rounded-full" />
            <div className="space-y-1.5">
              <Skeleton className="h-4 w-16" />
              <Skeleton className="h-3 w-12" />
            </div>
          </div>
          <div className="space-y-1.5 text-right">
            <Skeleton className="ml-auto h-4 w-24" />
            <Skeleton className="ml-auto h-3 w-16" />
          </div>
        </div>
      ))}
    </div>
  );
}

function FeaturedCoinCard({ coin, onClick }: { coin: CoinData; onClick: () => void }) {
  const colors = COIN_COLORS[coin.symbol] ?? { bg: 'bg-gray-50', text: 'text-gray-600', accent: 'border-gray-200' };
  const isUp = (coin.change24h ?? 0) > 0;
  const ChangeIcon = isUp ? ArrowUpRight : ArrowDownRight;

  return (
    <Card
      className={cn('rounded-xl border bg-white shadow-none cursor-pointer transition-colors hover:bg-gray-50', colors.accent)}
      onClick={onClick}
    >
      <CardContent className="p-5 space-y-3">
        <div className="flex items-center gap-3">
          <div className={cn('flex h-10 w-10 items-center justify-center rounded-full', colors.bg)}>
            <span className={cn('text-sm font-bold', colors.text)}>{coin.symbol.slice(0, 2)}</span>
          </div>
          <div>
            <p className="text-sm font-semibold text-gray-900">{coin.name}</p>
            <p className="text-xs text-gray-400">{coin.symbol}</p>
          </div>
        </div>
        <p className="text-xl font-bold text-gray-900 tabular-nums">{formatUsd(coin.priceUsd)}</p>
        <div className="flex items-center gap-1.5">
          <ChangeIcon className={cn('h-4 w-4', getChangeColor(coin.change24h))} />
          <span className={cn('text-sm font-semibold tabular-nums', getChangeColor(coin.change24h))}>
            {formatChange(coin.change24h)}
          </span>
          <span className="text-xs text-gray-400 ml-1">24h</span>
        </div>
        <p className="text-xs text-gray-500 tabular-nums">{formatKrw(coin.priceKrw)}</p>
        <div className="flex items-center gap-4 text-[11px] text-gray-400">
          <span>시총 {formatMarketCap(coin.marketCapUsd)}</span>
          <span>거래량 {formatVolume(coin.volume24hUsd)}</span>
        </div>
      </CardContent>
    </Card>
  );
}

function CoinDetailContent({ coin }: { coin: CoinData }) {
  return (
    <div className="space-y-5 px-4 py-2">
      <section>
        <div className="flex items-center gap-2 mb-3">
          <div className={cn('flex h-8 w-8 items-center justify-center rounded-full', COIN_COLORS[coin.symbol]?.bg ?? 'bg-gray-100')}>
            <span className={cn('text-xs font-bold', COIN_COLORS[coin.symbol]?.text ?? 'text-gray-600')}>
              {coin.symbol.slice(0, 2)}
            </span>
          </div>
          <div>
            <p className="text-base font-bold text-gray-900">{coin.name}</p>
            <p className="text-xs text-gray-400">{coin.symbol}</p>
          </div>
        </div>
        <div className="rounded-xl bg-gray-50 p-4 space-y-3">
          <div className="flex items-baseline justify-between">
            <span className="text-xs text-gray-500">USD 가격</span>
            <span className="text-lg font-bold text-gray-900 tabular-nums">{formatUsd(coin.priceUsd)}</span>
          </div>
          <div className="flex items-baseline justify-between">
            <span className="text-xs text-gray-500">KRW 가격</span>
            <span className="text-base font-semibold text-gray-800 tabular-nums">{formatKrw(coin.priceKrw)}</span>
          </div>
        </div>
      </section>

      <section>
        <h3 className="text-xs font-semibold text-gray-500 mb-2">변동률</h3>
        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-xl bg-gray-50 p-3">
            <p className="text-[11px] text-gray-400 mb-1">24시간</p>
            <p className={cn('text-sm font-bold tabular-nums', getChangeColor(coin.change24h))}>
              {formatChange(coin.change24h)}
            </p>
          </div>
          <div className="rounded-xl bg-gray-50 p-3">
            <p className="text-[11px] text-gray-400 mb-1">7일</p>
            <p className={cn('text-sm font-bold tabular-nums', getChangeColor(coin.change7d))}>
              {formatChange(coin.change7d)}
            </p>
          </div>
        </div>
      </section>

      <section>
        <h3 className="text-xs font-semibold text-gray-500 mb-2">시장 데이터</h3>
        <div className="space-y-2">
          <div className="flex items-center justify-between rounded-xl bg-gray-50 px-4 py-3">
            <span className="text-xs text-gray-500">시가총액</span>
            <span className="text-sm font-semibold text-gray-900 tabular-nums">{formatMarketCap(coin.marketCapUsd)}</span>
          </div>
          <div className="flex items-center justify-between rounded-xl bg-gray-50 px-4 py-3">
            <span className="text-xs text-gray-500">24시간 거래량</span>
            <span className="text-sm font-semibold text-gray-900 tabular-nums">{formatVolume(coin.volume24hUsd)}</span>
          </div>
        </div>
      </section>
    </div>
  );
}

export default function CryptoPage() {
  const { data: cryptoData, isLoading } = useCryptoList();
  const [selectedCoin, setSelectedCoin] = useState<CoinData | null>(null);
  const [sheetOpen, setSheetOpen] = useState(false);

  const coins = cryptoData?.coins ?? [];
  const featured = coins.filter((c) => FEATURED_SYMBOLS.includes(c.symbol));
  const others = coins.filter((c) => !FEATURED_SYMBOLS.includes(c.symbol));

  const handleCoinClick = (coin: CoinData) => {
    setSelectedCoin(coin);
    setSheetOpen(true);
  };

  const handleSheetOpenChange = (open: boolean) => {
    setSheetOpen(open);
    if (!open) setSelectedCoin(null);
  };

  return (
    <div className="mx-auto max-w-screen-xl p-4 md:p-6 space-y-4">
      <div className="flex items-center gap-2">
        <Bitcoin className="h-5 w-5 text-amber-500" />
        <h1 className="text-xl font-bold text-gray-900">암호화폐</h1>
      </div>

      {isLoading ? (
        <FeaturedSkeleton />
      ) : featured.length > 0 ? (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {featured.map((coin) => (
            <FeaturedCoinCard key={coin.symbol} coin={coin} onClick={() => handleCoinClick(coin)} />
          ))}
        </div>
      ) : null}

      <div>
        <h2 className="text-sm font-semibold text-gray-700 mb-2">전체 코인</h2>
        {isLoading ? (
          <CoinListSkeleton />
        ) : coins.length > 0 ? (
          <>

            <div className="hidden md:flex items-center px-4 py-2 text-[11px] font-medium text-gray-400 border-b border-gray-100">
              <div className="w-8 text-center">#</div>
              <div className="flex-1 ml-3">코인</div>
              <div className="w-32 text-right">가격 (USD)</div>
              <div className="w-28 text-right">가격 (KRW)</div>
              <div className="w-24 text-right">24h</div>
              <div className="w-24 text-right">7d</div>
              <div className="w-28 text-right">시가총액</div>
              <div className="w-28 text-right">거래량 (24h)</div>
            </div>

            <Card className="rounded-xl border-0 bg-white shadow-none">
              <CardContent className="divide-y divide-gray-100 p-0">
                {coins.map((coin, index) => (
                  <button
                    key={coin.symbol}
                    type="button"
                    className="flex w-full items-center px-4 py-3 text-left transition-colors hover:bg-gray-50"
                    onClick={() => handleCoinClick(coin)}
                  >

                    <div className="flex flex-1 items-center gap-3 md:hidden">
                      <div className={cn('flex h-8 w-8 shrink-0 items-center justify-center rounded-full', COIN_COLORS[coin.symbol]?.bg ?? 'bg-gray-100')}>
                        <span className={cn('text-[10px] font-bold', COIN_COLORS[coin.symbol]?.text ?? 'text-gray-600')}>
                          {coin.symbol.slice(0, 3)}
                        </span>
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-gray-900 truncate">{coin.name}</p>
                        <p className="text-[11px] text-gray-400">{coin.symbol}</p>
                      </div>
                      <div className="text-right shrink-0">
                        <p className="text-sm font-semibold text-gray-900 tabular-nums">{formatUsd(coin.priceUsd)}</p>
                        <p className={cn('text-xs font-medium tabular-nums', getChangeColor(coin.change24h))}>
                          {formatChange(coin.change24h)}
                        </p>
                      </div>
                    </div>

                    <div className="hidden md:flex flex-1 items-center">
                      <div className="w-8 text-center text-xs text-gray-400 tabular-nums">{index + 1}</div>
                      <div className="flex flex-1 items-center gap-2 ml-3">
                        <div className={cn('flex h-7 w-7 shrink-0 items-center justify-center rounded-full', COIN_COLORS[coin.symbol]?.bg ?? 'bg-gray-100')}>
                          <span className={cn('text-[9px] font-bold', COIN_COLORS[coin.symbol]?.text ?? 'text-gray-600')}>
                            {coin.symbol.slice(0, 3)}
                          </span>
                        </div>
                        <div className="min-w-0">
                          <span className="text-sm font-medium text-gray-900">{coin.name}</span>
                          <span className="ml-1.5 text-xs text-gray-400">{coin.symbol}</span>
                        </div>
                      </div>
                      <div className="w-32 text-right text-sm font-semibold text-gray-900 tabular-nums">{formatUsd(coin.priceUsd)}</div>
                      <div className="w-28 text-right text-xs text-gray-500 tabular-nums">{formatKrw(coin.priceKrw)}</div>
                      <div className={cn('w-24 text-right text-sm font-medium tabular-nums', getChangeColor(coin.change24h))}>
                        {formatChange(coin.change24h)}
                      </div>
                      <div className={cn('w-24 text-right text-sm font-medium tabular-nums', getChangeColor(coin.change7d))}>
                        {formatChange(coin.change7d)}
                      </div>
                      <div className="w-28 text-right text-xs text-gray-500 tabular-nums">{formatMarketCap(coin.marketCapUsd)}</div>
                      <div className="w-28 text-right text-xs text-gray-500 tabular-nums">{formatVolume(coin.volume24hUsd)}</div>
                    </div>
                  </button>
                ))}
              </CardContent>
            </Card>
          </>
        ) : (
          <div className="py-16 text-center text-sm text-gray-400">암호화폐 데이터가 없습니다</div>
        )}
      </div>

      <Sheet open={sheetOpen} onOpenChange={handleSheetOpenChange}>
        <SheetContent side="bottom" className="mx-auto max-w-screen-md rounded-t-2xl max-h-[85vh] overflow-y-auto">
          <div className="flex justify-center pt-2 pb-1">
            <div className="h-1 w-10 rounded-full bg-gray-300" />
          </div>
          <SheetHeader className="px-4 pb-0">
            <SheetTitle className="text-base font-bold text-gray-900">
              {selectedCoin?.name ?? '코인 상세'}
            </SheetTitle>
            <SheetDescription asChild>
              <div className="flex items-center gap-2 pt-1">
                {selectedCoin && (
                  <>
                    <Badge variant="outline" className="text-[10px] px-1.5 py-0 bg-gray-50 text-gray-600 border-gray-200">
                      {selectedCoin.symbol}
                    </Badge>
                    <span className={cn('text-xs font-medium tabular-nums', getChangeColor(selectedCoin.change24h))}>
                      24h {formatChange(selectedCoin.change24h)}
                    </span>
                  </>
                )}
              </div>
            </SheetDescription>
          </SheetHeader>
          {selectedCoin && <CoinDetailContent coin={selectedCoin} />}
        </SheetContent>
      </Sheet>
    </div>
  );
}
