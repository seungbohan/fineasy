// SEO constants and utilities for FinEasy
export const SITE_URL = 'https://fineasy.kr';
export const SITE_NAME = 'FinEasy';
export const SITE_DESCRIPTION =
  '금융 초보자도 쉽게 이해하는 주식 분석, 금융 용어 사전, AI 리포트, 거시경제 지표를 제공하는 금융 정보 플랫폼입니다.';
export const SITE_LOCALE = 'ko_KR';

export const DEFAULT_OG_IMAGE = `${SITE_URL}/og-image.png`;

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
