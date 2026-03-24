'use client';

import { useState, useRef, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { useRouter, usePathname } from 'next/navigation';
import {
  Search,
  User,
  LogOut,
  X,
} from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/stores/auth-store';
import { useSearchStore } from '@/stores/search-store';
import { apiClient } from '@/lib/api-client';
import { cn } from '@/lib/utils';
import { FinancialTerm } from '@/types';

interface StockSearchResult {
  id: number;
  stockCode: string;
  stockName: string;
  market: string;
  sector: string;
}

export function Header() {
  const router = useRouter();
  const pathname = usePathname();
  const { isAuthenticated, user, logout } = useAuthStore();
  const { recentSearches, addSearch } = useSearchStore();

  const [searchOpen, setSearchOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<{
    stocks: StockSearchResult[];
    terms: FinancialTerm[];
  }>({ stocks: [], terms: [] });
  const inputRef = useRef<HTMLInputElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const performSearch = useCallback(async (q: string) => {
    if (!q.trim()) {
      setResults({ stocks: [], terms: [] });
      return;
    }

    try {
      const [stocks, terms] = await Promise.allSettled([
        apiClient.get<StockSearchResult[]>(
          `/stocks/search?q=${encodeURIComponent(q)}`
        ),
        apiClient.get<FinancialTerm[]>(
          `/terms/search?q=${encodeURIComponent(q)}`
        ),
      ]);

      setResults({
        stocks:
          stocks.status === 'fulfilled' ? stocks.value.slice(0, 5) : [],
        terms:
          terms.status === 'fulfilled' ? terms.value.slice(0, 3) : [],
      });
    } catch {
      setResults({ stocks: [], terms: [] });
    }
  }, []);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => performSearch(query), 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query, performSearch]);

  useEffect(() => {
    if (searchOpen && inputRef.current) {
      inputRef.current.focus();
    }
  }, [searchOpen]);

  const handleSelect = (type: 'stock' | 'term', id: string, label: string) => {
    addSearch(label);
    setSearchOpen(false);
    setQuery('');
    if (type === 'stock') {
      router.push(`/stocks/${id}`);
    } else {
      router.push(`/dictionary/${id}`);
    }
  };

  const showDropdown =
    searchOpen && (query.length > 0 || recentSearches.length > 0);

  return (
    <header className="sticky top-0 z-40 bg-white/80 backdrop-blur-xl shadow-[0_1px_3px_rgba(0,0,0,0.04)]">
      <div className="mx-auto flex h-16 max-w-screen-xl items-center gap-3 px-4">
        <Link
          href="/"
          className="flex shrink-0 items-center gap-1.5 font-bold text-xl transition-opacity hover:opacity-80"
          aria-label="FinEasy 홈으로 이동"
        >
          <span className="text-[#3182F6]">Fin</span>
          <span>Easy</span>
        </Link>

        <nav className="hidden items-center gap-0.5 md:flex ml-4" aria-label="주요 메뉴">
          {[
            { href: '/news', label: '뉴스' },
            { href: '/global-events', label: '글로벌' },
            { href: '/stocks', label: '종목' },
            { href: '/macro', label: '거시경제' },
            { href: '/crypto', label: '암호화폐' },
            { href: '/dictionary', label: '용어사전' },
            { href: '/learn', label: '학습센터' },
          ].map((item) => {
            const isActive = pathname === item.href || pathname.startsWith(item.href + '/');
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'rounded-lg px-3 py-2 text-sm font-semibold transition-colors',
                  isActive
                    ? 'bg-gray-900/5 text-[#3182F6]'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                )}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="flex-1" />

        <div className="relative">
          {searchOpen ? (
            <div className="flex items-center gap-2">
              <div className="relative">
                <Input
                  ref={inputRef}
                  type="text"
                  placeholder="종목명, 종목코드, 용어 검색"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  className="h-9 w-48 pr-8 text-sm sm:w-64"
                  aria-label="통합 검색"
                  onKeyDown={(e) => {
                    if (e.key === 'Escape') {
                      setSearchOpen(false);
                      setQuery('');
                    }
                  }}
                />
                <button
                  onClick={() => {
                    setSearchOpen(false);
                    setQuery('');
                  }}
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  aria-label="검색 닫기"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            </div>
          ) : (
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setSearchOpen(true)}
              aria-label="검색 열기"
            >
              <Search className="h-5 w-5" />
            </Button>
          )}

          {showDropdown && (
            <div className="absolute right-0 top-full mt-2 w-72 rounded-2xl border border-gray-100 bg-white shadow-[0_8px_30px_rgba(0,0,0,0.08)] sm:w-80 animate-in fade-in slide-in-from-top-2 duration-200">
              {query.length === 0 && recentSearches.length > 0 && (
                <div className="p-3">
                  <p className="mb-2 text-xs font-medium text-gray-500">
                    최근 검색어
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {recentSearches.map((s) => (
                      <button
                        key={s}
                        onClick={() => setQuery(s)}
                        className="rounded-full bg-gray-100 px-3 py-1 text-xs text-gray-700 hover:bg-gray-200 transition-colors"
                      >
                        {s}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {results.stocks.length > 0 && (
                <div className="border-b p-2">
                  <p className="px-2 pb-1 text-xs font-medium text-gray-500">
                    종목
                  </p>
                  {results.stocks.map((stock) => (
                    <button
                      key={stock.stockCode}
                      onClick={() =>
                        handleSelect('stock', stock.stockCode, stock.stockName)
                      }
                      className="flex w-full items-center justify-between rounded-md px-2 py-2 text-sm hover:bg-gray-50 transition-colors"
                    >
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{stock.stockName}</span>
                        <span className="text-xs text-gray-400">
                          {stock.stockCode}
                        </span>
                      </div>
                      <span className="text-xs text-gray-400">
                        {stock.market}
                      </span>
                    </button>
                  ))}
                </div>
              )}

              {results.terms.length > 0 && (
                <div className="p-2">
                  <p className="px-2 pb-1 text-xs font-medium text-gray-500">
                    금융 용어
                  </p>
                  {results.terms.map((term) => (
                    <button
                      key={term.id}
                      onClick={() =>
                        handleSelect('term', String(term.id), term.name)
                      }
                      className="flex w-full flex-col items-start rounded-md px-2 py-2 text-sm hover:bg-gray-50 transition-colors"
                    >
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{term.name}</span>
                        <span className="text-xs text-gray-400">
                          {term.nameEn}
                        </span>
                      </div>
                      <p className="mt-0.5 text-xs text-gray-500 line-clamp-1 text-left">
                        {term.simpleDescription}
                      </p>
                    </button>
                  ))}
                </div>
              )}

              {query.length > 0 &&
                results.stocks.length === 0 &&
                results.terms.length === 0 && (
                  <div className="p-4 text-center text-sm text-gray-500">
                    검색 결과가 없습니다
                  </div>
                )}
            </div>
          )}
        </div>

        {isAuthenticated ? (
          <div className="flex items-center gap-2">
            <Link href="/mypage">
              <Button variant="ghost" size="icon" aria-label="마이페이지">
                <User className="h-5 w-5" />
              </Button>
            </Link>
            <Button
              variant="ghost"
              size="icon"
              onClick={logout}
              aria-label="로그아웃"
              className="hidden sm:inline-flex"
            >
              <LogOut className="h-5 w-5" />
            </Button>
          </div>
        ) : (
          <Link href="/login">
            <Button variant="outline" size="sm" className="text-sm">
              로그인
            </Button>
          </Link>
        )}
      </div>
    </header>
  );
}
