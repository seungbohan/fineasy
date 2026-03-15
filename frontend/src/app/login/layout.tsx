import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '로그인',
  description: 'FinEasy에 로그인하여 관심 종목 관리, AI 브리핑 등 맞춤 서비스를 이용하세요.',
  path: '/login',
  noIndex: true,
});

export default function LoginLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
