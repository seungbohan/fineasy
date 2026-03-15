'use client';

import { use, useState } from 'react';
import Link from 'next/link';
import {
  ArrowLeft,
  ChevronDown,
  ChevronUp,
  Lightbulb,
  BookOpen,
  Quote,
  Link2,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { DifficultyBadge } from '@/components/shared/difficulty-badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useTermDetail } from '@/hooks/use-terms';

export default function TermDetailPage({
  params,
}: {
  params: Promise<{ termId: string }>;
}) {
  const { termId } = use(params);
  const id = parseInt(termId, 10);
  const { data: term, isLoading } = useTermDetail(id);
  const [showDetail, setShowDetail] = useState(false);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-screen-xl px-4 py-4 md:px-6 md:py-6">
        <Skeleton className="mb-4 h-5 w-24" />
        <div className="rounded-2xl bg-white p-5 shadow-sm">
          <Skeleton className="mb-2 h-6 w-48" />
          <Skeleton className="mb-1 h-4 w-32" />
          <div className="mt-4 flex gap-2">
            <Skeleton className="h-5 w-12 rounded-full" />
            <Skeleton className="h-5 w-16 rounded-full" />
          </div>
        </div>
        <div className="mt-3 space-y-3">
          <Skeleton className="h-24 w-full rounded-2xl" />
          <Skeleton className="h-16 w-full rounded-2xl" />
        </div>
      </div>
    );
  }

  if (!term) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-gray-100">
          <BookOpen className="h-6 w-6 text-gray-400" />
        </div>
        <p className="text-sm font-medium text-gray-700">
          용어를 찾을 수 없습니다
        </p>
        <Link href="/dictionary">
          <Button variant="outline" className="rounded-full text-sm">
            용어 사전으로 돌아가기
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-screen-xl px-4 py-4 md:px-6 md:py-6">
      <Link
        href="/dictionary"
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 transition-colors"
        aria-label="용어 사전으로 돌아가기"
      >
        <ArrowLeft className="h-4 w-4" />
        용어 사전
      </Link>

      <div className="rounded-2xl bg-white shadow-sm overflow-hidden">
        <div className="border-b border-gray-100 bg-gradient-to-b from-green-50/40 to-white px-5 pb-4 pt-5">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <DifficultyBadge difficulty={term.difficulty} />
            <Badge
              variant="outline"
              className="border-gray-200 bg-white px-2 py-0.5 text-[10px] font-normal text-gray-500"
            >
              {term.category}
            </Badge>
          </div>

          <h1 className="text-xl font-bold text-gray-900 md:text-2xl">
            {term.name}
          </h1>

          {term.nameEn && (
            <p className="mt-1 text-sm text-gray-400">{term.nameEn}</p>
          )}
        </div>

        <div className="px-5 py-5">
          <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-900">
            <Lightbulb className="h-4 w-4 text-yellow-500" />
            쉬운 설명
          </h2>
          <p className="text-sm leading-[1.8] text-gray-700">
            {term.simpleDescription}
          </p>
        </div>
      </div>

      <div className="mt-3 rounded-2xl bg-white shadow-sm overflow-hidden">
        <button
          onClick={() => setShowDetail(!showDetail)}
          className="flex w-full items-center justify-between px-5 py-4 transition-colors hover:bg-gray-50"
          aria-expanded={showDetail}
        >
          <h2 className="flex items-center gap-2 text-sm font-semibold text-gray-900">
            <BookOpen className="h-4 w-4 text-blue-500" />
            상세 설명
          </h2>
          <div className="flex h-6 w-6 items-center justify-center rounded-full bg-gray-100">
            {showDetail ? (
              <ChevronUp className="h-3.5 w-3.5 text-gray-500" />
            ) : (
              <ChevronDown className="h-3.5 w-3.5 text-gray-500" />
            )}
          </div>
        </button>
        {showDetail && (
          <div className="border-t border-gray-100 px-5 pb-5 pt-4">
            <p className="text-sm leading-[1.8] text-gray-600">
              {term.detailedDescription}
            </p>
          </div>
        )}
      </div>

      {term.exampleSentence && (
        <div className="mt-3 rounded-2xl bg-blue-50/60 shadow-sm overflow-hidden">
          <div className="px-5 py-5">
            <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-900">
              <Quote className="h-4 w-4 text-blue-500" />
              활용 예시
            </h2>
            <p className="rounded-xl bg-white px-4 py-3 text-sm leading-relaxed text-gray-700 italic">
              &ldquo;{term.exampleSentence}&rdquo;
            </p>
          </div>
        </div>
      )}

      {term.relatedTerms.length > 0 && (
        <div className="mt-3 rounded-2xl bg-white shadow-sm overflow-hidden">
          <div className="px-5 py-5">
            <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-900">
              <Link2 className="h-4 w-4 text-gray-500" />
              관련 용어
            </h2>
            <div className="flex flex-wrap gap-2">
              {term.relatedTerms.map((related) => (
                <Link
                  key={related.id}
                  href={`/dictionary/${related.id}`}
                  className="rounded-full border border-gray-200 bg-gray-50 px-3.5 py-1.5 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-100 hover:border-gray-300 active:bg-gray-200"
                >
                  {related.name}
                </Link>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
