import type { MetadataRoute } from 'next';

const SITE_URL = 'https://fineasy.co.kr';

// Force dynamic generation at runtime, not build time
export const dynamic = 'force-dynamic';

const API_URL = process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://backend:8080/api/v1';

async function fetchJson<T>(path: string): Promise<T | null> {
  try {
    console.log(`[Sitemap] Fetching: ${API_URL}${path}`);
    const res = await fetch(`${API_URL}${path}`, {
      signal: AbortSignal.timeout(10000),
    });
    if (!res.ok) {
      console.error(`[Sitemap] Failed: ${res.status} for ${path}`);
      return null;
    }
    const json = await res.json();
    console.log(`[Sitemap] Success: ${path}, entries: ${Array.isArray(json.data ?? json) ? (json.data ?? json).length : 'object'}`);
    return json.data ?? json;
  } catch (error) {
    console.error(`[Sitemap] Error fetching ${path}:`, error);
    return null;
  }
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const staticPages: MetadataRoute.Sitemap = [
    { url: SITE_URL, lastModified: new Date(), changeFrequency: 'daily', priority: 1.0 },
    { url: `${SITE_URL}/news`, lastModified: new Date(), changeFrequency: 'hourly', priority: 0.9 },
    { url: `${SITE_URL}/stocks`, lastModified: new Date(), changeFrequency: 'daily', priority: 0.9 },
    { url: `${SITE_URL}/dictionary`, lastModified: new Date(), changeFrequency: 'weekly', priority: 0.8 },
    { url: `${SITE_URL}/learn`, lastModified: new Date(), changeFrequency: 'weekly', priority: 0.8 },
    { url: `${SITE_URL}/macro`, lastModified: new Date(), changeFrequency: 'daily', priority: 0.7 },
    { url: `${SITE_URL}/global-events`, lastModified: new Date(), changeFrequency: 'hourly', priority: 0.7 },
    { url: `${SITE_URL}/crypto`, lastModified: new Date(), changeFrequency: 'hourly', priority: 0.7 },
  ];

  const dynamicEntries: MetadataRoute.Sitemap = [];

  // Dynamic stock pages
  try {
    const stocks = await fetchJson<{ stockCode: string }[]>('/stocks/popular?size=200');
    if (stocks && Array.isArray(stocks)) {
      for (const s of stocks) {
        dynamicEntries.push({
          url: `${SITE_URL}/stocks/${s.stockCode}`,
          lastModified: new Date(),
          changeFrequency: 'daily',
          priority: 0.7,
        });
      }
    }
  } catch { /* skip */ }

  // Dynamic BOK term pages
  try {
    const terms = await fetchJson<{ content: { id: number }[] }>('/bok-terms?page=0&size=700');
    if (terms?.content) {
      for (const t of terms.content) {
        dynamicEntries.push({
          url: `${SITE_URL}/dictionary/bok/${t.id}`,
          lastModified: new Date(),
          changeFrequency: 'monthly',
          priority: 0.6,
        });
      }
    }
  } catch { /* skip */ }

  // Dynamic learn article pages
  try {
    const articles = await fetchJson<{ content: { id: number }[] }>('/learn/articles?page=0&size=100');
    if (articles?.content) {
      for (const a of articles.content) {
        dynamicEntries.push({
          url: `${SITE_URL}/learn/${a.id}`,
          lastModified: new Date(),
          changeFrequency: 'monthly',
          priority: 0.6,
        });
      }
    }
  } catch { /* skip */ }

  return [...staticPages, ...dynamicEntries];
}
