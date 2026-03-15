import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';
import { BreadcrumbJsonLd } from '@/components/seo/json-ld';
import { SITE_URL } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '거시경제 지표 - FRED, ECOS, 한국은행 데이터',
  description:
    'FRED, ECOS, 한국은행 데이터 기반 거시경제 지표를 실시간으로 확인하세요. 기준금리, 환율, CPI, 원자재 가격 등 주요 경제 지표를 한눈에 제공합니다.',
  path: '/macro',
});

export default function MacroLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <BreadcrumbJsonLd
        items={[
          { name: '홈', url: SITE_URL },
          { name: '거시경제 지표', url: `${SITE_URL}/macro` },
        ]}
      />
      {children}
    </>
  );
}
