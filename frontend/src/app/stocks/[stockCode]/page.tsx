import type { Metadata } from 'next';
import { createPageMetadata, SITE_URL } from '@/lib/seo';
import { BreadcrumbJsonLd } from '@/components/seo/json-ld';
import StockDetailPage from './stock-detail-client';

const API_URL = process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

interface Props {
  params: Promise<{ stockCode: string }>;
}

interface StockData {
  stockName: string;
  stockCode: string;
  currentPrice?: number;
  changeRate?: number;
  marketCap?: number;
  volume?: number;
}

async function fetchStock(stockCode: string): Promise<StockData | null> {
  try {
    const res = await fetch(`${API_URL}/stocks/${stockCode}`, {
      next: { revalidate: 86400 },
    });
    if (!res.ok) return null;
    const json = await res.json();
    return json.data ?? null;
  } catch {
    return null;
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { stockCode } = await params;
  const stock = await fetchStock(stockCode);
  const stockName = stock?.stockName ?? null;

  const isKorean = /^\d{6}$/.test(stockCode);
  const displayName = stockName ? `${stockName}(${stockCode})` : stockCode;

  const title = stockName
    ? `${stockName} 주가 분석 - AI 리포트, 실시간 시세`
    : `${stockCode} 종목 상세 - 실시간 시세, AI 분석`;

  const description = stockName
    ? `${displayName}의 실시간 주가, AI 종목 분석, 재무제표, 관련 뉴스를 확인하세요. ${isKorean ? 'KRX/KOSDAQ' : 'NASDAQ/NYSE'} 상장 종목의 투자 정보를 FinEasy에서 무료로 제공합니다.`
    : `${stockCode} 종목의 실시간 시세, 차트, AI 분석 리포트를 확인하세요.`;

  return createPageMetadata({
    title,
    description,
    path: `/stocks/${stockCode}`,
  });
}

export default async function Page({ params }: Props) {
  const { stockCode } = await params;
  const stock = await fetchStock(stockCode);
  const displayName = stock?.stockName || stockCode;

  return (
    <>
      <BreadcrumbJsonLd
        items={[
          { name: '홈', url: SITE_URL },
          { name: '인기 종목', url: `${SITE_URL}/stocks` },
          { name: displayName, url: `${SITE_URL}/stocks/${stockCode}` },
        ]}
      />
      {stock && (
        <section className="sr-only" aria-label="종목 정보">
          <h1>{stock.stockName} ({stock.stockCode})</h1>
          <p>{stock.stockName}의 실시간 주가, AI 종목 분석, 재무제표, 관련 뉴스를 확인하세요.</p>
          {stock.currentPrice != null && <p>현재가: {stock.currentPrice.toLocaleString()}원</p>}
          {stock.changeRate != null && <p>등락률: {stock.changeRate}%</p>}
        </section>
      )}
      <StockDetailPage params={params} />
    </>
  );
}
