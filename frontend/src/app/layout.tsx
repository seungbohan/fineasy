import type { Metadata, Viewport } from 'next';
import { Noto_Sans_KR, Geist_Mono } from 'next/font/google';
import './globals.css';
import { Providers } from '@/components/providers';
import { Header } from '@/components/layout/header';
import { BottomNav } from '@/components/layout/bottom-nav';
import {
  WebsiteJsonLd,
  OrganizationJsonLd,
  FinancialServiceJsonLd,
} from '@/components/seo/json-ld';
import {
  SITE_URL,
  SITE_NAME,
  SITE_DESCRIPTION,
  SITE_LOCALE,
  DEFAULT_OG_IMAGE,
} from '@/lib/seo';
import { AdSenseScript } from '@/components/adsense-script';

const notoSansKR = Noto_Sans_KR({
  variable: '--font-noto-sans-kr',
  subsets: ['latin'],
  weight: ['300', '400', '500', '600', '700'],
});

const geistMono = Geist_Mono({
  variable: '--font-geist-mono',
  subsets: ['latin'],
});

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 5,
  themeColor: '#ffffff',
};

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: `${SITE_NAME} - 주식 초보를 위한 AI 종목 분석, 금융 용어 사전`,
    template: `%s | ${SITE_NAME}`,
  },
  description: SITE_DESCRIPTION,
  keywords: [
    '주식 초보',
    '주식 시작',
    '주식 용어 정리',
    'ETF 시작',
    '사회초년생 재테크',
    '금융 용어 사전',
    '주식 분석',
    'AI 주가 분석',
    'PER PBR 뜻',
    '코스피 코스닥 차이',
    '삼성전자 주가',
    '금융 뉴스',
    '거시경제 지표',
    '암호화폐 시세',
    '투자 학습',
    '한국은행 경제금융용어',
    '주가 예측',
    '코스피',
    '코스닥',
    'NASDAQ',
  ],
  authors: [{ name: SITE_NAME, url: SITE_URL }],
  creator: SITE_NAME,
  publisher: SITE_NAME,
  formatDetection: {
    telephone: false,
    email: false,
  },
  alternates: {
    canonical: SITE_URL,
  },
  openGraph: {
    type: 'website',
    locale: SITE_LOCALE,
    url: SITE_URL,
    siteName: SITE_NAME,
    title: `${SITE_NAME} - 초보자를 위한 금융 정보 플랫폼`,
    description: SITE_DESCRIPTION,
    images: [
      {
        url: DEFAULT_OG_IMAGE,
        width: 1200,
        height: 630,
        alt: `${SITE_NAME} - 초보자를 위한 금융 정보 플랫폼`,
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: `${SITE_NAME} - 초보자를 위한 금융 정보 플랫폼`,
    description: SITE_DESCRIPTION,
    images: [DEFAULT_OG_IMAGE],
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      'max-video-preview': -1,
      'max-image-preview': 'large',
      'max-snippet': -1,
    },
  },
  verification: {
    google: 'google57ae7b5280904b67',
    other: { 'naver-site-verification': 'naver0c71701f7d1796e9283cf9dbb6ded66a' },
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <head>
        <WebsiteJsonLd />
        <OrganizationJsonLd />
        <FinancialServiceJsonLd />
        <script
          async
          src={`https://www.googletagmanager.com/gtag/js?id=G-NLYGDP45K8`}
        />
        <script
          dangerouslySetInnerHTML={{
            __html: `
              window.dataLayer = window.dataLayer || [];
              function gtag(){dataLayer.push(arguments);}
              gtag('js', new Date());
              gtag('config', 'G-NLYGDP45K8');
            `,
          }}
        />
      </head>
      <body
        className={`${notoSansKR.variable} ${geistMono.variable} antialiased`}
      >
        <AdSenseScript />
        <Providers>
          <div className="flex min-h-screen flex-col">
            <Header />
            <main className="flex-1 pb-[68px] md:pb-0">{children}</main>
            <BottomNav />
          </div>
        </Providers>
      </body>
    </html>
  );
}
