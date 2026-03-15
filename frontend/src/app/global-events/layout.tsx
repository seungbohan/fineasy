import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';
import { BreadcrumbJsonLd } from '@/components/seo/json-ld';
import { SITE_URL } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '글로벌 이벤트 - 시장 리스크 모니터링',
  description:
    '글로벌 경제 이벤트와 시장 리스크를 실시간으로 모니터링합니다. 지정학적 리스크, 재정 정책, 산업 이슈, 블랙스완 이벤트를 추적하세요.',
  path: '/global-events',
});

export default function GlobalEventsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <BreadcrumbJsonLd
        items={[
          { name: '홈', url: SITE_URL },
          { name: '글로벌 이벤트', url: `${SITE_URL}/global-events` },
        ]}
      />
      {children}
    </>
  );
}
