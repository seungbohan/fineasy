import type { Metadata } from 'next';
import { createPageMetadata } from '@/lib/seo';
import { BreadcrumbJsonLd } from '@/components/seo/json-ld';
import { SITE_URL } from '@/lib/seo';

export const metadata: Metadata = createPageMetadata({
  title: '금융 뉴스 - AI 감성 분석',
  description:
    '최신 금융 뉴스를 AI가 분석합니다. 긍정/부정/중립 감성 분류와 시장 영향도 분석으로 투자 판단에 도움을 받으세요.',
  path: '/news',
});

export default function NewsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <BreadcrumbJsonLd
        items={[
          { name: '홈', url: SITE_URL },
          { name: '금융 뉴스', url: `${SITE_URL}/news` },
        ]}
      />
      {children}
    </>
  );
}
