import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';
import { BreadcrumbJsonLd } from '@/components/seo/json-ld';
import { SITE_URL } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '암호화폐 시세 - 비트코인, 이더리움 실시간 가격',
  description:
    '비트코인, 이더리움 등 주요 암호화폐의 실시간 시세를 USD/KRW로 확인하세요. 24시간 변동률, 시가총액, 거래량 정보를 제공합니다.',
  path: '/crypto',
});

export default function CryptoLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <BreadcrumbJsonLd
        items={[
          { name: '홈', url: SITE_URL },
          { name: '암호화폐', url: `${SITE_URL}/crypto` },
        ]}
      />
      {children}
    </>
  );
}
