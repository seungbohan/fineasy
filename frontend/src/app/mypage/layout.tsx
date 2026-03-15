import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '마이페이지',
  description: '관심 종목 관리, 계정 설정 등 나만의 FinEasy를 관리하세요.',
  path: '/mypage',
  noIndex: true,
});

export default function MypageLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
