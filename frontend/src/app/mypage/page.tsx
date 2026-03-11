'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { User, LogOut, Heart, ChevronRight } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/stores/auth-store';
import { useWatchlistStore, WatchlistItem } from '@/stores/watchlist-store';
import { apiClient } from '@/lib/api-client';
import {
  formatPrice,
  formatChangeRate,
  getPriceColorClass,
} from '@/lib/format';

export default function MyPage() {
  const router = useRouter();
  const { isAuthenticated, user, logout } = useAuthStore();
  const { removeStock, fetchWatchlist } = useWatchlistStore();

  useEffect(() => {
    if (!isAuthenticated) {
      router.replace('/login');
    }
  }, [isAuthenticated, router]);

  const { data: serverItems = [] } = useQuery<WatchlistItem[]>({
    queryKey: ['mypage', 'watchlist'],
    queryFn: () => apiClient.get<WatchlistItem[]>('/watchlist'),
    enabled: isAuthenticated,
  });

  const watchlistCodes = serverItems.map((item) => item.stockCode);
  const { data: watchedStocks = [], refetch } = useQuery<{
    stockCode: string;
    stockName: string;
    currentPrice: number;
    changeAmount: number;
    changeRate: number;
  }[]>({
    queryKey: ['mypage', 'watchlist', 'prices', watchlistCodes],
    queryFn: async () => {
      if (watchlistCodes.length === 0) return [];
      const results = await Promise.allSettled(
        watchlistCodes.map((code) =>
          apiClient.get<{ stockCode: string; stockName: string; currentPrice: number; changeAmount: number; changeRate: number }>(`/stocks/${code}/price`)
        )
      );
      return results
        .filter((r): r is PromiseFulfilledResult<{ stockCode: string; stockName: string; currentPrice: number; changeAmount: number; changeRate: number }> => r.status === 'fulfilled')
        .map((r) => r.value);
    },
    enabled: watchlistCodes.length > 0,
  });

  useEffect(() => {
    if (isAuthenticated) {
      fetchWatchlist();
    }
  }, [isAuthenticated, fetchWatchlist]);

  if (!isAuthenticated || !user) {
    return null;
  }

  const handleLogout = () => {
    logout();
    router.push('/');
  };

  return (
    <div className="mx-auto max-w-screen-xl p-4 md:p-6 space-y-4">
      <h1 className="text-xl font-bold text-gray-900">마이페이지</h1>

      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[#3182F6]/10">
              <User className="h-6 w-6 text-[#3182F6]" />
            </div>
            <div>
              <p className="text-base font-semibold text-gray-900">
                {user.nickname}
              </p>
              <p className="text-sm text-gray-500">{user.email}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-gray-900">
              관심 종목 관리
            </h2>
            <span className="text-xs text-gray-400">
              {watchedStocks.length}개
            </span>
          </div>

          {watchedStocks.length === 0 ? (
            <div className="py-6 text-center">
              <p className="text-sm text-gray-400">
                관심 종목이 없습니다
              </p>
              <Link href="/" className="mt-2 inline-block">
                <Button variant="outline" size="sm" className="text-xs">
                  종목 둘러보기
                </Button>
              </Link>
            </div>
          ) : (
            <div className="divide-y divide-gray-100">
              {watchedStocks.map((item) => (
                <div
                  key={item.stockCode}
                  className="flex items-center justify-between py-3"
                >
                  <Link
                    href={`/stocks/${item.stockCode}`}
                    className="flex-1 min-w-0"
                  >
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-gray-900">
                        {item.stockName}
                      </span>
                      <span className="text-xs text-gray-400">
                        {item.stockCode}
                      </span>
                    </div>
                    <div className="mt-0.5 flex items-center gap-2">
                      <span className="text-xs tabular-nums text-gray-600">
                        {formatPrice(item.currentPrice)}원
                      </span>
                      <span
                        className={`text-xs tabular-nums ${getPriceColorClass(
                          item.changeRate
                        )}`}
                      >
                        {formatChangeRate(item.changeRate)}
                      </span>
                    </div>
                  </Link>
                  <button
                    onClick={async () => {
                      await removeStock(item.stockCode);
                      refetch();
                    }}
                    className="ml-3 text-red-400 hover:text-red-500 transition-colors"
                    aria-label={`${item.stockName} 관심 종목 해제`}
                  >
                    <Heart className="h-4 w-4 fill-current" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="divide-y divide-gray-100 p-0">
          <Link
            href="/learn"
            className="flex items-center justify-between px-4 py-3.5 transition-colors hover:bg-gray-50"
          >
            <span className="text-sm text-gray-700">학습 센터</span>
            <ChevronRight className="h-4 w-4 text-gray-300" />
          </Link>
          <Link
            href="/dictionary"
            className="flex items-center justify-between px-4 py-3.5 transition-colors hover:bg-gray-50"
          >
            <span className="text-sm text-gray-700">용어 사전</span>
            <ChevronRight className="h-4 w-4 text-gray-300" />
          </Link>
        </CardContent>
      </Card>

      <Button
        variant="outline"
        className="w-full text-red-500 border-red-200 hover:bg-red-50 hover:text-red-600"
        onClick={handleLogout}
      >
        <LogOut className="mr-2 h-4 w-4" />
        로그아웃
      </Button>
    </div>
  );
}
