import type { Metadata } from 'next';
import { createPageMetadata, SITE_URL } from '@/lib/seo';
import { BreadcrumbJsonLd, DefinedTermJsonLd } from '@/components/seo/json-ld';
import BokTermDetailPage from './bok-term-detail-client';

const API_URL = process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

interface Props {
  params: Promise<{ termId: string }>;
}

interface BokTermData {
  id: number;
  term: string;
  englishTerm?: string;
  definition: string;
}

async function fetchBokTerm(termId: string): Promise<BokTermData | null> {
  try {
    const res = await fetch(`${API_URL}/bok-terms/${termId}`, {
      next: { revalidate: 3600 },
    });
    if (!res.ok) return null;
    const json = await res.json();
    return json.data ?? json;
  } catch {
    return null;
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { termId } = await params;
  const term = await fetchBokTerm(termId);

  const title = term
    ? `${term.term} 뜻 - 한국은행 경제금융용어 AI 쉬운 설명`
    : '한국은행 경제금융용어 상세 - AI 쉬운 설명';
  const description = term
    ? `${term.term}${term.englishTerm ? `(${term.englishTerm})` : ''}: ${term.definition.slice(0, 120)}`
    : '한국은행 경제금융용어 700선의 상세 정의와 AI 쉬운 설명을 확인하세요.';

  return createPageMetadata({
    title,
    description,
    path: `/dictionary/bok/${termId}`,
  });
}

export default async function Page({ params }: Props) {
  const { termId } = await params;
  const term = await fetchBokTerm(termId);

  return (
    <>
      <BreadcrumbJsonLd
        items={[
          { name: '홈', url: SITE_URL },
          { name: '금융 용어 사전', url: `${SITE_URL}/dictionary` },
          { name: term?.term || '한국은행 용어 상세', url: `${SITE_URL}/dictionary/bok/${termId}` },
        ]}
      />
      {term && (
        <DefinedTermJsonLd
          name={term.term}
          description={term.definition.slice(0, 200)}
          url={`${SITE_URL}/dictionary/bok/${termId}`}
        />
      )}
      {term && (
        <section className="sr-only" aria-label="한국은행 용어 정보">
          <h1>{term.term}{term.englishTerm && ` (${term.englishTerm})`}</h1>
          <p>출처: 한국은행 경제금융용어 700선</p>
          <h2>정의</h2>
          <p>{term.definition}</p>
        </section>
      )}
      <BokTermDetailPage params={params} />
    </>
  );
}
