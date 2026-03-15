import type { Metadata } from 'next';
import { createPageMetadata, SITE_URL } from '@/lib/seo';
import { BreadcrumbJsonLd } from '@/components/seo/json-ld';
import BokTermDetailPage from './bok-term-detail-client';

interface Props {
  params: Promise<{ termId: string }>;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { termId } = await params;

  return createPageMetadata({
    title: `한국은행 경제금융용어 상세 - AI 쉬운 설명`,
    description: `한국은행 경제금융용어 700선의 상세 정의와 AI 쉬운 설명을 확인하세요. 어려운 경제 용어를 초보자도 이해할 수 있게 풀어드립니다.`,
    path: `/dictionary/bok/${termId}`,
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
          { name: '한국은행 용어 상세', url: `${SITE_URL}/dictionary/bok/${termId}` },
        ]}
      />
      <BokTermDetailPage params={params} />
    </>
  );
}
