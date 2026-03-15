import type { MetadataRoute } from 'next';

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: '*',
        allow: '/',
        disallow: ['/mypage', '/login', '/signup', '/api/'],
      },
      {
        userAgent: 'Yeti', // Naver crawler
        allow: '/',
        disallow: ['/mypage', '/login', '/signup', '/api/'],
      },
    ],
    sitemap: 'https://fineasy.kr/sitemap.xml',
    host: 'https://fineasy.kr',
  };
}
