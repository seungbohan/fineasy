'use client';

import { use, useState } from 'react';
import Link from 'next/link';
import {
  ArrowLeft,
  BookOpen,
  ExternalLink,
  Sparkles,
  Lightbulb,
  MessageCircle,
  ListChecks,
  Loader2,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useBokTermDetail, useBokTermExplanation } from '@/hooks/use-bok-terms';

export default function BokTermDetailPage({
  params,
}: {
  params: Promise<{ termId: string }>;
}) {
  const { termId } = use(params);
  const id = parseInt(termId, 10);
  const { data: term, isLoading } = useBokTermDetail(id);

  const [aiRequested, setAiRequested] = useState(false);
  const {
    data: explanation,
    isLoading: isAiLoading,
    isError: isAiError,
  } = useBokTermExplanation(id, aiRequested);

  const [showOriginal, setShowOriginal] = useState(false);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-screen-xl px-4 py-4 md:px-6 md:py-6">
        <Skeleton className="mb-4 h-5 w-24" />
        <div className="rounded-2xl bg-white p-5 shadow-sm">
          <Skeleton className="mb-2 h-6 w-48" />
          <Skeleton className="mb-1 h-4 w-32" />
          <Skeleton className="mb-4 h-5 w-20 rounded-full" />
          <div className="space-y-2 pt-4">
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-3 w-3/4" />
          </div>
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
        <div className="border-b border-gray-100 bg-gradient-to-b from-blue-50/50 to-white px-5 pb-4 pt-5">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <Badge
              variant="outline"
              className="border-blue-200 bg-blue-50 px-2 py-0.5 text-[10px] font-semibold text-blue-700"
            >
              BOK 경제금융용어
            </Badge>
          </div>
          <h1 className="text-xl font-bold text-gray-900 md:text-2xl">
            {term.term}
          </h1>
          {term.englishTerm && (
            <p className="mt-1 text-sm text-gray-400">{term.englishTerm}</p>
          )}
        </div>
      </div>

      <div className="mt-3">
        {!aiRequested ? (

          <button
            onClick={() => setAiRequested(true)}
            className="w-full rounded-2xl bg-gradient-to-r from-violet-500 to-blue-500 p-4 text-left text-white shadow-sm transition-all hover:shadow-md active:scale-[0.99]"
          >
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white/20">
                <Sparkles className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm font-semibold">AI 쉬운 설명 보기</p>
                <p className="mt-0.5 text-xs text-white/80">
                  어려운 용어를 중학생도 이해할 수 있게 풀어드려요
                </p>
              </div>
            </div>
          </button>
        ) : isAiLoading ? (

          <div className="rounded-2xl bg-white p-5 shadow-sm">
            <div className="flex items-center gap-3 text-violet-600">
              <Loader2 className="h-5 w-5 animate-spin" />
              <p className="text-sm font-medium">
                AI가 쉬운 설명을 만들고 있어요...
              </p>
            </div>
            <div className="mt-4 space-y-3">
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-3 w-full" />
              <Skeleton className="h-3 w-full" />
              <Skeleton className="h-3 w-2/3" />
            </div>
          </div>
        ) : isAiError ? (

          <div className="rounded-2xl bg-white p-5 shadow-sm">
            <p className="text-sm text-gray-500">
              AI 설명을 불러오지 못했어요. 다시 시도해주세요.
            </p>
            <Button
              variant="outline"
              size="sm"
              className="mt-3 rounded-full text-xs"
              onClick={() => setAiRequested(false)}
            >
              다시 시도
            </Button>
          </div>
        ) : explanation ? (

          <div className="space-y-3">

            <div className="rounded-2xl bg-gradient-to-br from-violet-50 to-blue-50 p-4 shadow-sm">
              <div className="mb-2 flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-violet-500" />
                <h2 className="text-sm font-semibold text-gray-900">
                  한 줄 요약
                </h2>
              </div>
              <p className="text-sm font-medium leading-relaxed text-gray-800">
                {explanation.simpleSummary}
              </p>
            </div>

            <div className="rounded-2xl bg-white p-4 shadow-sm">
              <div className="mb-2 flex items-center gap-2">
                <Lightbulb className="h-4 w-4 text-amber-500" />
                <h2 className="text-sm font-semibold text-gray-900">
                  쉬운 풀이
                </h2>
              </div>
              <p className="text-sm leading-[1.8] text-gray-700">
                {explanation.easyExplanation}
              </p>
            </div>

            <div className="rounded-2xl bg-blue-50/60 p-4 shadow-sm">
              <div className="mb-2 flex items-center gap-2">
                <MessageCircle className="h-4 w-4 text-blue-500" />
                <h2 className="text-sm font-semibold text-gray-900">
                  예시로 이해하기
                </h2>
              </div>
              <div className="rounded-xl bg-white p-3">
                <p className="text-sm leading-[1.8] text-gray-700 italic">
                  &ldquo;{explanation.example}&rdquo;
                </p>
              </div>
            </div>

            {explanation.keyPoints && explanation.keyPoints.length > 0 && (
              <div className="rounded-2xl bg-white p-4 shadow-sm">
                <div className="mb-3 flex items-center gap-2">
                  <ListChecks className="h-4 w-4 text-green-500" />
                  <h2 className="text-sm font-semibold text-gray-900">
                    핵심 포인트
                  </h2>
                </div>
                <ul className="space-y-2">
                  {explanation.keyPoints.map((point, i) => (
                    <li key={i} className="flex items-start gap-2">
                      <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-green-50 text-[10px] font-bold text-green-600">
                        {i + 1}
                      </span>
                      <span className="text-sm leading-relaxed text-gray-700">
                        {point}
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <p className="px-1 text-[11px] text-gray-400">
              <Sparkles className="mr-1 inline h-3 w-3" />
              AI가 생성한 설명으로, 원본 정의와 다소 다를 수 있습니다
            </p>
          </div>
        ) : null}
      </div>

      <div className="mt-3 rounded-2xl bg-white shadow-sm overflow-hidden">
        <button
          onClick={() => setShowOriginal(!showOriginal)}
          className="flex w-full items-center justify-between px-5 py-4 text-left transition-colors hover:bg-gray-50"
          aria-expanded={showOriginal}
        >
          <h2 className="flex items-center gap-2 text-sm font-semibold text-gray-900">
            <BookOpen className="h-4 w-4 text-blue-500" />
            원문 정의
          </h2>
          {showOriginal ? (
            <ChevronUp className="h-4 w-4 text-gray-400" />
          ) : (
            <ChevronDown className="h-4 w-4 text-gray-400" />
          )}
        </button>
        {showOriginal && (
          <div className="border-t border-gray-100 px-5 pb-5 pt-3">
            <p className="text-sm leading-[1.8] text-gray-600 whitespace-pre-line">
              {term.definition}
            </p>
          </div>
        )}

        <div className="border-t border-gray-100 px-5 py-3">
          <p className="flex items-center gap-1.5 text-[11px] text-gray-400">
            <ExternalLink className="h-3 w-3" />
            출처: 한국은행 경제금융용어 700선
          </p>
        </div>
      </div>
    </div>
  );
}
