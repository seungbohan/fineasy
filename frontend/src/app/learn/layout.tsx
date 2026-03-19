import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '주식 초보 시작 가이드 - ETF 처음 시작, 사회초년생 재테크',
  description:
    '주식 초보 시작하는 법, ETF 처음 시작하기, 사회초년생 재테크 가이드. 투자 기초부터 차트 분석까지 단계별로 배워보세요. 무료 금융 교육 콘텐츠.',
  path: '/learn',
});

export default function LearnLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
