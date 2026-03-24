'use client';

import { usePathname } from 'next/navigation';
import Script from 'next/script';

const NO_ADS_PATHS = ['/login', '/signup', '/mypage'];

export function AdSenseScript() {
  const pathname = usePathname();

  const isNoAdsPage = NO_ADS_PATHS.some(
    (path) => pathname === path || pathname.startsWith(path + '/')
  );

  if (isNoAdsPage) return null;

  return (
    <Script
      async
      src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-1140877243889064"
      crossOrigin="anonymous"
      strategy="afterInteractive"
    />
  );
}
