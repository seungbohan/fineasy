import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '투자 학습 센터 - 투자 기초부터 차트 분석까지',
  description:
    '투자 기초, 뉴스 읽는 법, 차트 분석, 가치 분석, 거시경제까지 단계별로 배워보세요. 금융 초보자를 위한 체계적인 투자 교육 콘텐츠를 제공합니다.',
  path: '/learn',
});

export default function LearnLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
