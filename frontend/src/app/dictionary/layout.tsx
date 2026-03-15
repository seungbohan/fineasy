import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '금융 용어 사전 - 경제금융용어 700선',
  description:
    '한국은행 경제금융용어 700선과 기초 금융 용어를 쉽게 검색하고 이해하세요. 초성 검색, 카테고리 분류, AI 쉬운 설명 기능을 제공합니다.',
  path: '/dictionary',
});

export default function DictionaryLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
