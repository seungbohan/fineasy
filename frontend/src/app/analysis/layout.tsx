import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';
import { BreadcrumbJsonLd } from '@/components/seo/json-ld';
import { SITE_URL } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: 'AI 주가 분석 - AI 종목 분석 리포트',
  description:
    'AI가 분석한 종목별 투자 리포트와 주가 방향성 예측을 확인하세요. 시장 분석, 기술적 분석, 뉴스 기반 분석을 종합적으로 제공합니다.',
  path: '/analysis',
});

export default function AnalysisLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <BreadcrumbJsonLd
        items={[
          { name: '홈', url: SITE_URL },
          { name: 'AI 주가 분석', url: `${SITE_URL}/analysis` },
        ]}
      />
      {children}
    </>
  );
}
