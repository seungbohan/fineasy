/**
 * @file Sector analysis listing page
 * @description Displays sector cards for various industries (semiconductor, defense, battery, etc.)
 *   Each card links to a detailed sector page with tabs for industry structure, value chain, trends, and top companies.
 */
'use client';

import Link from 'next/link';
import {
  Cpu,
  Shield,
  Battery,
  Pill,
  Landmark,
  Flame,
  ArrowLeft,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';

/* ------------------------------------------------------------------ */
/*  Sector configuration                                               */
/* ------------------------------------------------------------------ */

interface SectorConfig {
  slug: string;
  name: string;
  icon: React.ElementType;
  color: string;
  bg: string;
  description: string;
  companiesCount: number;
}

const SECTORS: SectorConfig[] = [
  {
    slug: 'semiconductor',
    name: '반도체',
    icon: Cpu,
    color: 'text-blue-600',
    bg: 'bg-blue-50',
    description: '메모리, 비메모리, 파운드리 등 반도체 산업 전반',
    companiesCount: 8,
  },
  {
    slug: 'defense',
    name: '방산',
    icon: Shield,
    color: 'text-slate-600',
    bg: 'bg-slate-100',
    description: '한국 방산 수출 및 국방 관련 기업 분석',
    companiesCount: 6,
  },
  {
    slug: 'battery',
    name: '이차전지',
    icon: Battery,
    color: 'text-green-600',
    bg: 'bg-green-50',
    description: '배터리 셀, 소재, 장비 등 이차전지 밸류체인',
    companiesCount: 7,
  },
  {
    slug: 'bio',
    name: '바이오',
    icon: Pill,
    color: 'text-purple-600',
    bg: 'bg-purple-50',
    description: '신약 개발, CDMO, 바이오시밀러 등 바이오 산업',
    companiesCount: 6,
  },
  {
    slug: 'finance',
    name: '금융',
    icon: Landmark,
    color: 'text-amber-600',
    bg: 'bg-amber-50',
    description: '은행, 보험, 증권 등 금융 섹터 분석',
    companiesCount: 6,
  },
  {
    slug: 'energy',
    name: '에너지',
    icon: Flame,
    color: 'text-orange-600',
    bg: 'bg-orange-50',
    description: '정유, 가스, 신재생에너지 관련 기업',
    companiesCount: 5,
  },
];

/* ------------------------------------------------------------------ */
/*  Main page component                                                */
/* ------------------------------------------------------------------ */

export default function SectorsPage() {
  return (
    <div className="mx-auto max-w-screen-xl space-y-4 p-4 md:p-6">
      {/* Back navigation */}
      <Link
        href="/learn"
        className="inline-flex items-center gap-1 text-xs text-gray-400 hover:text-gray-600 transition-colors"
      >
        <ArrowLeft className="h-3 w-3" />
        학습센터로 돌아가기
      </Link>

      {/* Page header */}
      <div>
        <h1 className="text-xl font-bold text-gray-900">섹터 분석</h1>
        <p className="mt-1 text-sm text-gray-500">
          주요 산업 섹터의 구조, 밸류체인, 동향 및 대표 기업을 알아보세요.
        </p>
      </div>

      {/* Sector cards */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {SECTORS.map((sector) => {
          const Icon = sector.icon;
          return (
            <Link key={sector.slug} href={`/learn/sectors/${sector.slug}`}>
              <Card className="rounded-xl border-0 bg-white shadow-none transition-colors hover:bg-gray-50">
                <CardContent className="flex items-start gap-3 p-4">
                  <div
                    className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${sector.bg}`}
                  >
                    <Icon className={`h-5 w-5 ${sector.color}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <h3 className="text-sm font-semibold text-gray-900">{sector.name}</h3>
                      <span className="text-[10px] text-gray-300">
                        대표기업 {sector.companiesCount}개
                      </span>
                    </div>
                    <p className="mt-0.5 text-xs text-gray-500 line-clamp-2">
                      {sector.description}
                    </p>
                  </div>
                </CardContent>
              </Card>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
