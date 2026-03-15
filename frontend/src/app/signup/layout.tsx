import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '회원가입',
  description: 'FinEasy에 가입하고 AI 기반 금융 분석과 맞춤 투자 정보를 받아보세요.',
  path: '/signup',
  noIndex: true,
});

export default function SignupLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
