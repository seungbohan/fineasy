/**
 * @file Calculator layout with SEO metadata
 * @description ETF compound interest calculator page layout
 */
import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: 'ETF 복리 수익률 계산기 - 장기 투자 시뮬레이션',
  description:
    'QQQ, SPY, SCHD 등 인기 ETF 과거 수익률 기반 복리 계산기. 월 적립 투자 시 미래 자산을 시뮬레이션해보세요.',
  path: '/calculator',
});

export default function CalculatorLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
