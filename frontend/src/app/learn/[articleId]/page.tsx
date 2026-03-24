import type { Metadata } from 'next';
import { createPageMetadata, SITE_URL } from '@/lib/seo';
import { BreadcrumbJsonLd } from '@/components/seo/json-ld';
import LearnArticlePage from './learn-article-client';

const API_URL = process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

interface Props {
  params: Promise<{ articleId: string }>;
}

interface ArticleData {
  id: number;
  title: string;
  content: string;
  difficulty: string;
  estimatedReadMinutes: number;
  categoryName?: string;
}

async function fetchArticle(articleId: string): Promise<ArticleData | null> {
  try {
    const res = await fetch(`${API_URL}/learn/articles/${articleId}`, {
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
  const { articleId } = await params;
  const article = await fetchArticle(articleId);

  const title = article
    ? `${article.title} - 투자 학습`
    : '투자 학습 - 금융 교육 콘텐츠';
  const description = article
    ? `${article.title}: ${article.content.replace(/[#*\n]/g, ' ').slice(0, 120)}`
    : '투자에 필요한 핵심 지식을 단계별로 배워보세요.';

  return createPageMetadata({
    title,
    description,
    path: `/learn/${articleId}`,
  });
}

export default async function Page({ params }: Props) {
  const { articleId } = await params;
  const article = await fetchArticle(articleId);

  return (
    <>
      <BreadcrumbJsonLd
        items={[
          { name: '홈', url: SITE_URL },
          { name: '투자 학습 센터', url: `${SITE_URL}/learn` },
          { name: article?.title || '학습 콘텐츠', url: `${SITE_URL}/learn/${articleId}` },
        ]}
      />
      {article && (
        <section className="sr-only" aria-label="학습 콘텐츠">
          <h1>{article.title}</h1>
          <p>난이도: {article.difficulty} | 읽기 시간: {article.estimatedReadMinutes}분</p>
          <div>{article.content}</div>
        </section>
      )}
      <LearnArticlePage params={params} />
    </>
  );
}
