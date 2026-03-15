import type { Metadata } from 'next';
import { createPageMetadata, SITE_URL } from '@/lib/seo';
import { BreadcrumbJsonLd } from '@/components/seo/json-ld';
import StockDetailPage from './stock-detail-client';

interface Props {
  params: Promise<{ stockCode: string }>;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { stockCode } = await params;

  // Dynamic metadata with stock code
  // In production, you could fetch stock name from API here
  return createPageMetadata({
    title: `${stockCode} 종목 상세 - 실시간 시세, AI 분석`,
    description: `${stockCode} 종목의 실시간 시세, 차트, 재무제표, AI 분석 리포트, 관련 뉴스를 확인하세요. FinEasy에서 투자에 필요한 모든 정보를 제공합니다.`,
    path: `/stocks/${stockCode}`,
  });
}

export default async function Page({ params }: Props) {
  const { stockCode } = await params;

  return (
    <>
      <BreadcrumbJsonLd
        items={[
          { name: '홈', url: SITE_URL },
          { name: '인기 종목', url: `${SITE_URL}/stocks` },
          { name: stockCode, url: `${SITE_URL}/stocks/${stockCode}` },
        ]}
      />
      <StockDetailPage params={params} />
    </>
  );
}
