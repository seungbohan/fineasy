import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '금융 용어 사전 - PER PBR 뜻, 주식 용어 정리, 한국은행 700선',
  description:
    'PER, PBR, EPS 뜻부터 한국은행 경제금융용어 700선까지. 주식 초보가 꼭 알아야 할 금융 용어를 AI가 쉽게 설명합니다. 초성 검색, 카테고리 분류 지원.',
  path: '/dictionary',
});

export default function DictionaryLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
