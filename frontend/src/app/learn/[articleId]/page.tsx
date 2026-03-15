import type { Metadata } from 'next';
import { createPageMetadata, SITE_URL } from '@/lib/seo';
import { BreadcrumbJsonLd } from '@/components/seo/json-ld';
import LearnArticlePage from './learn-article-client';

interface Props {
  params: Promise<{ articleId: string }>;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { articleId } = await params;

  return createPageMetadata({
    title: `투자 학습 - 금융 교육 콘텐츠`,
    description: `투자에 필요한 핵심 지식을 단계별로 배워보세요. 초보자부터 중급자까지 맞춤 금융 교육 콘텐츠를 제공합니다.`,
    path: `/learn/${articleId}`,
  });
}

export default async function Page({ params }: Props) {
  const { articleId } = await params;

  return (
    <>
      <BreadcrumbJsonLd
        items={[
          { name: '홈', url: SITE_URL },
          { name: '투자 학습 센터', url: `${SITE_URL}/learn` },
          { name: '학습 콘텐츠', url: `${SITE_URL}/learn/${articleId}` },
        ]}
      />
      <LearnArticlePage params={params} />
    </>
  );
}
