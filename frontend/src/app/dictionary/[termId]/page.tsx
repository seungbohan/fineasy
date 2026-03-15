import type { Metadata } from 'next';
import { createPageMetadata, SITE_URL } from '@/lib/seo';
import { BreadcrumbJsonLd } from '@/components/seo/json-ld';
import TermDetailPage from './term-detail-client';

interface Props {
  params: Promise<{ termId: string }>;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { termId } = await params;

  return createPageMetadata({
    title: `금융 용어 상세 - 쉬운 설명과 예시`,
    description: `금융 용어를 쉽게 이해하세요. 초보자도 알기 쉬운 설명, 상세 해설, 활용 예시와 관련 용어를 제공합니다.`,
    path: `/dictionary/${termId}`,
  });
}

export default async function Page({ params }: Props) {
  const { termId } = await params;

  return (
    <>
      <BreadcrumbJsonLd
        items={[
          { name: '홈', url: SITE_URL },
          { name: '금융 용어 사전', url: `${SITE_URL}/dictionary` },
          { name: '용어 상세', url: `${SITE_URL}/dictionary/${termId}` },
        ]}
      />
      <TermDetailPage params={params} />
    </>
  );
}
