'use client';

import { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  Home,
  TrendingUp,
  MoreHorizontal,
  BookOpen,
  GraduationCap,
  Globe,
  Bitcoin,
  LineChart,
  Newspaper,
} from 'lucide-react';
import { cn } from '@/lib/utils';

const PRIMARY_ITEMS = [
  { href: '/', label: '홈', icon: Home },
  { href: '/news', label: '뉴스', icon: Newspaper },
  { href: '/global-events', label: '글로벌', icon: Globe },
  { href: '/stocks', label: '종목', icon: TrendingUp },
];

const MORE_ITEMS = [
  { href: '/macro', label: '거시경제', icon: LineChart },
  { href: '/crypto', label: '암호화폐', icon: Bitcoin },
  { href: '/dictionary', label: '용어사전', icon: BookOpen },
  { href: '/learn', label: '학습센터', icon: GraduationCap },
];

const MORE_HREFS = MORE_ITEMS.map((item) => item.href);

export function BottomNav() {
  const pathname = usePathname();
  const [moreOpen, setMoreOpen] = useState(false);
  const moreRef = useRef<HTMLDivElement>(null);


  useEffect(() => {
    setMoreOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!moreOpen) return;

    function handleClickOutside(e: MouseEvent) {
      if (moreRef.current && !moreRef.current.contains(e.target as Node)) {
        setMoreOpen(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [moreOpen]);


  const isMoreActive = MORE_HREFS.some((href) => pathname.startsWith(href));

  return (
    <nav
      className="fixed bottom-0 left-0 right-0 z-40 bg-white/90 backdrop-blur-xl shadow-[0_-1px_3px_rgba(0,0,0,0.04)] md:hidden"
      aria-label="하단 메뉴"
    >
      <div className="flex h-[68px] items-center justify-around">

        {PRIMARY_ITEMS.map((item) => {
          const isActive =
            item.href === '/'
              ? pathname === '/'
              : pathname.startsWith(item.href);
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                'flex min-w-[56px] flex-col items-center gap-0.5 py-1 transition-all duration-200',
                isActive
                  ? 'text-[#3182F6] scale-105'
                  : 'text-gray-400 hover:text-gray-600'
              )}
              aria-current={isActive ? 'page' : undefined}
            >
              <Icon className="h-[22px] w-[22px]" />
              <span className="text-[11px] font-semibold">{item.label}</span>
              {isActive && (
                <span className="h-1 w-1 rounded-full bg-[#3182F6]" />
              )}
            </Link>
          );
        })}

        <div className="relative" ref={moreRef}>
          <button
            type="button"
            className={cn(
              'flex min-w-[56px] flex-col items-center gap-1 py-1 transition-colors',
              isMoreActive
                ? 'text-[#3182F6]'
                : 'text-gray-400 hover:text-gray-600'
            )}
            onClick={() => setMoreOpen((prev) => !prev)}
            aria-expanded={moreOpen}
            aria-haspopup="true"
            aria-label="더보기 메뉴"
          >
            <MoreHorizontal className="h-[22px] w-[22px]" />
            <span className="text-[11px] font-semibold">더보기</span>
          </button>

          {moreOpen && (
            <div className="absolute bottom-full right-0 mb-2 w-44 rounded-2xl border border-gray-100 bg-white/95 backdrop-blur-xl py-1.5 shadow-[0_8px_30px_rgba(0,0,0,0.1)] animate-in fade-in slide-in-from-bottom-2 duration-200">
              {MORE_ITEMS.map((item) => {
                const isActive = pathname.startsWith(item.href);
                const Icon = item.icon;

                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      'flex items-center gap-3 px-4 py-2.5 text-sm transition-colors',
                      isActive
                        ? 'text-[#3182F6] bg-blue-50/50'
                        : 'text-gray-600 hover:bg-gray-50'
                    )}
                    aria-current={isActive ? 'page' : undefined}
                  >
                    <Icon className="h-4 w-4 shrink-0" />
                    <span className="font-medium">{item.label}</span>
                  </Link>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
