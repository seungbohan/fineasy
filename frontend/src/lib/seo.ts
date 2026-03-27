// SEO constants and utilities for FinEasy
export const SITE_URL = 'https://fineasy.co.kr';
export const SITE_NAME = 'FinEasy';
export const SITE_DESCRIPTION =
  '주식 초보도 쉽게 시작하는 AI 종목 분석, 금융 용어 사전(한국은행 700선), 실시간 시세, 투자 학습. 사회초년생 재테크의 첫걸음, FinEasy에서 무료로 시작하세요.';
export const SITE_LOCALE = 'ko_KR';

export const DEFAULT_OG_IMAGE = `${SITE_URL}/opengraph-image`;

export function createCanonicalUrl(path: string): string {
  return `${SITE_URL}${path}`;
}

export function createPageMetadata({
  title,
  description,
  path,
  ogImage,
  noIndex = false,
}: {
  title: string;
  description: string;
  path: string;
  ogImage?: string;
  noIndex?: boolean;
}) {
  const canonicalUrl = createCanonicalUrl(path);
  const image = ogImage || DEFAULT_OG_IMAGE;

  return {
    title,
    description,
    alternates: {
      canonical: canonicalUrl,
    },
    openGraph: {
      title,
      description,
      url: canonicalUrl,
      siteName: SITE_NAME,
      locale: SITE_LOCALE,
      type: 'website' as const,
      images: [
        {
          url: image,
          width: 1200,
          height: 630,
          alt: title,
        },
      ],
    },
    twitter: {
      card: 'summary_large_image' as const,
      title,
      description,
      images: [image],
    },
    ...(noIndex && {
      robots: {
        index: false,
        follow: false,
      },
    }),
  };
}
