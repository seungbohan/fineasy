import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '인기 종목 랭킹 - 거래대금, 거래량, 상승률, 하락률',
  description:
    '국내외 인기 종목을 거래대금, 거래량, 상승률, 하락률 기준으로 실시간 랭킹합니다. 코스피, 코스닥, 미국 주식을 한눈에 확인하세요.',
  path: '/stocks',
});

export default function StocksLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
