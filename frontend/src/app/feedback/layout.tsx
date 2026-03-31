/**
 * @file Feedback layout with SEO metadata
 * @description Feedback submission page layout
 */
import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '피드백 및 개선사항 접수',
  description:
    'FinEasy 서비스 개선을 위한 피드백을 남겨주세요. 버그 리포트, 기능 제안, 불편사항 등을 접수합니다.',
  path: '/feedback',
});

export default function FeedbackLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
