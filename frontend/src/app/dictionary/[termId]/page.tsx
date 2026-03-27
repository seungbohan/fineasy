import type { Metadata } from 'next';
import { createPageMetadata, SITE_URL } from '@/lib/seo';
import { BreadcrumbJsonLd, DefinedTermJsonLd } from '@/components/seo/json-ld';
import TermDetailPage from './term-detail-client';

const API_URL = process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

interface Props {
  params: Promise<{ termId: string }>;
}

interface TermData {
  id: number;
  name: string;
  nameEn?: string;
  category: string;
  difficulty: string;
  simpleDescription: string;
  detailedDescription: string;
  exampleSentence?: string;
}

async function fetchTerm(termId: string): Promise<TermData | null> {
  try {
    const res = await fetch(`${API_URL}/terms/${termId}`, {
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
  const term = await fetchTerm(termId);

  const title = term
    ? `${term.name} 뜻 - 쉬운 설명과 예시`
    : '금융 용어 상세 - 쉬운 설명과 예시';
  const description = term
    ? `${term.name}${term.nameEn ? `(${term.nameEn})` : ''}: ${term.simpleDescription.slice(0, 120)}`
    : '금융 용어를 쉽게 이해하세요. 초보자도 알기 쉬운 설명, 상세 해설, 활용 예시와 관련 용어를 제공합니다.';

  return createPageMetadata({
    title,
    description,
    path: `/dictionary/${termId}`,
  });
}

export default async function Page({ params }: Props) {
  const { termId } = await params;
  const term = await fetchTerm(termId);

  return (
    <>
      <BreadcrumbJsonLd
        items={[
          { name: '홈', url: SITE_URL },
          { name: '금융 용어 사전', url: `${SITE_URL}/dictionary` },
          { name: term?.name || '용어 상세', url: `${SITE_URL}/dictionary/${termId}` },
        ]}
      />
      {term && (
        <DefinedTermJsonLd
          name={term.name}
          description={term.simpleDescription}
          url={`${SITE_URL}/dictionary/${termId}`}
        />
      )}
      {term && (
        <section className="sr-only" aria-label="용어 정보">
          <h1>{term.name}{term.nameEn && ` (${term.nameEn})`}</h1>
          <p>카테고리: {term.category} | 난이도: {term.difficulty}</p>
          <h2>쉬운 설명</h2>
          <p>{term.simpleDescription}</p>
          <h2>상세 설명</h2>
          <p>{term.detailedDescription}</p>
          {term.exampleSentence && (
            <>
              <h2>활용 예시</h2>
              <p>{term.exampleSentence}</p>
            </>
          )}
        </section>
      )}
      <TermDetailPage params={params} />
    </>
  );
}
