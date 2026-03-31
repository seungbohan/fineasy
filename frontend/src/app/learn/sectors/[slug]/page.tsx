/**
 * @file Sector detail page with tabbed content
 * @description Displays detailed analysis for a specific sector including:
 *   - Industry structure overview
 *   - Value chain breakdown
 *   - Current trends
 *   - Representative companies (with stock page links when stock code is available)
 */
'use client';

import { use } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  ArrowLeft,
  Building2,
  GitBranch,
  TrendingUp,
  Users,
  ExternalLink,
  Cpu,
  Shield,
  Battery,
  Pill,
  Landmark,
  Flame,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { SECTOR_DATA } from '../sector-data';

/* ------------------------------------------------------------------ */
/*  Icon mapping                                                       */
/* ------------------------------------------------------------------ */

const SECTOR_ICON: Record<string, React.ElementType> = {
  semiconductor: Cpu,
  defense: Shield,
  battery: Battery,
  bio: Pill,
  finance: Landmark,
  energy: Flame,
};

const SECTOR_COLORS: Record<string, { color: string; bg: string }> = {
  semiconductor: { color: 'text-blue-600', bg: 'bg-blue-50' },
  defense: { color: 'text-slate-600', bg: 'bg-slate-100' },
  battery: { color: 'text-green-600', bg: 'bg-green-50' },
  bio: { color: 'text-purple-600', bg: 'bg-purple-50' },
  finance: { color: 'text-amber-600', bg: 'bg-amber-50' },
  energy: { color: 'text-orange-600', bg: 'bg-orange-50' },
};

/* ------------------------------------------------------------------ */
/*  Main page component                                                */
/* ------------------------------------------------------------------ */

export default function SectorDetailPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = use(params);
  const router = useRouter();
  const sector = SECTOR_DATA[slug];

  if (!sector) {
    return (
      <div className="mx-auto max-w-screen-xl p-4 md:p-6 text-center">
        <p className="text-sm text-gray-500">존재하지 않는 섹터입니다.</p>
        <Link href="/learn/sectors" className="mt-4 inline-block text-sm text-[#3182F6]">
          섹터 목록으로 돌아가기
        </Link>
      </div>
    );
  }

  const Icon = SECTOR_ICON[slug] || Building2;
  const colors = SECTOR_COLORS[slug] || { color: 'text-gray-600', bg: 'bg-gray-100' };

  return (
    <div className="mx-auto max-w-screen-xl space-y-4 p-4 md:p-6">
      {/* Back navigation */}
      <button
        type="button"
        onClick={() => router.back()}
        className="inline-flex items-center gap-1 text-xs text-gray-400 hover:text-gray-600 transition-colors"
      >
        <ArrowLeft className="h-3 w-3" />
        뒤로가기
      </button>

      {/* Header */}
      <div className="flex items-center gap-3">
        <div className={`flex h-11 w-11 items-center justify-center rounded-xl ${colors.bg}`}>
          <Icon className={`h-5 w-5 ${colors.color}`} />
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{sector.name} 섹터 분석</h1>
          <p className="text-xs text-gray-500">산업 구조, 밸류체인, 동향, 대표 기업</p>
        </div>
      </div>

      {/* Overview */}
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <p className="text-sm leading-relaxed text-gray-700">{sector.overview}</p>
        </CardContent>
      </Card>

      {/* Tabs */}
      <Tabs defaultValue="structure" className="space-y-3">
        <TabsList className="w-full" variant="line">
          <TabsTrigger value="structure" className="flex-1 text-xs">
            <Building2 className="mr-1 h-3.5 w-3.5" />
            산업 구조
          </TabsTrigger>
          <TabsTrigger value="valuechain" className="flex-1 text-xs">
            <GitBranch className="mr-1 h-3.5 w-3.5" />
            밸류체인
          </TabsTrigger>
          <TabsTrigger value="trends" className="flex-1 text-xs">
            <TrendingUp className="mr-1 h-3.5 w-3.5" />
            동향
          </TabsTrigger>
          <TabsTrigger value="companies" className="flex-1 text-xs">
            <Users className="mr-1 h-3.5 w-3.5" />
            대표 기업
          </TabsTrigger>
        </TabsList>

        {/* Industry structure tab */}
        <TabsContent value="structure">
          <Card className="rounded-xl border-0 bg-white shadow-none">
            <CardContent className="p-4">
              <div className="mb-3 flex items-center gap-2">
                <Building2 className="h-4 w-4 text-[#3182F6]" />
                <span className="text-sm font-semibold text-gray-900">산업 구조</span>
              </div>
              <ul className="space-y-2.5">
                {sector.structure.map((item, i) => (
                  <li key={i} className="flex items-start gap-2">
                    <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-[#3182F6]" />
                    <span className="text-sm text-gray-700">{item}</span>
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Value chain tab */}
        <TabsContent value="valuechain">
          <Card className="rounded-xl border-0 bg-white shadow-none">
            <CardContent className="p-4">
              <div className="mb-3 flex items-center gap-2">
                <GitBranch className="h-4 w-4 text-[#3182F6]" />
                <span className="text-sm font-semibold text-gray-900">밸류체인</span>
              </div>
              <div className="space-y-0">
                {sector.valueChain.map((step, i) => (
                  <div key={i} className="relative flex gap-3 pb-4 last:pb-0">
                    {/* Vertical connector line */}
                    {i < sector.valueChain.length - 1 && (
                      <div className="absolute left-[11px] top-6 h-full w-0.5 bg-blue-100" />
                    )}
                    {/* Step number circle */}
                    <div className="relative z-10 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[#3182F6] text-[10px] font-bold text-white">
                      {i + 1}
                    </div>
                    <div className="flex-1 pt-0.5">
                      <p className="text-sm font-medium text-gray-900">{step.step}</p>
                      <p className="mt-0.5 text-xs text-gray-500">{step.description}</p>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Trends tab */}
        <TabsContent value="trends">
          <div className="space-y-3">
            {sector.trends.map((trend, i) => (
              <Card key={i} className="rounded-xl border-0 bg-white shadow-none">
                <CardContent className="p-4">
                  <div className="mb-2 flex items-center gap-2">
                    <TrendingUp className="h-4 w-4 text-[#3182F6]" />
                    <span className="text-sm font-semibold text-gray-900">{trend.title}</span>
                  </div>
                  <p className="text-sm leading-relaxed text-gray-600">{trend.content}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        {/* Companies tab */}
        <TabsContent value="companies">
          <Card className="rounded-xl border-0 bg-white shadow-none">
            <CardContent className="divide-y divide-gray-100 p-0">
              {sector.companies.map((company) => {
                const inner = (
                  <div className="flex items-center justify-between px-4 py-3.5">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-medium text-gray-900">{company.name}</p>
                        <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-medium text-gray-500">
                          {company.role}
                        </span>
                      </div>
                      <p className="mt-0.5 text-xs text-gray-500">{company.description}</p>
                    </div>
                    {company.stockCode && (
                      <ExternalLink className="h-3.5 w-3.5 shrink-0 text-gray-300" />
                    )}
                  </div>
                );

                if (company.stockCode) {
                  return (
                    <Link
                      key={company.name}
                      href={`/stocks/${company.stockCode}`}
                      className="block transition-colors hover:bg-gray-50"
                    >
                      {inner}
                    </Link>
                  );
                }

                return (
                  <div key={company.name} className="opacity-75">
                    {inner}
                  </div>
                );
              })}
            </CardContent>
          </Card>
          <p className="mt-2 px-1 text-[10px] text-gray-400">
            종목코드가 있는 기업을 탭하면 종목 상세 페이지로 이동합니다.
          </p>
        </TabsContent>
      </Tabs>
    </div>
  );
}
