/**
 * @file Feedback submission page
 * @description Allows users (logged-in or anonymous) to submit feedback including
 *   bug reports, feature requests, complaints, and general feedback.
 *   Prevents duplicate submissions by disabling the submit button after success.
 */
'use client';

import { useState, useCallback } from 'react';
import {
  MessageSquarePlus,
  Bug,
  Lightbulb,
  AlertTriangle,
  HelpCircle,
  Send,
  CheckCircle2,
  ArrowLeft,
} from 'lucide-react';
import Link from 'next/link';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { apiClient } from '@/lib/api-client';

/* ------------------------------------------------------------------ */
/*  Feedback category configuration                                    */
/* ------------------------------------------------------------------ */

interface FeedbackCategory {
  value: string;
  label: string;
  icon: React.ElementType;
  color: string;
  bg: string;
}

const FEEDBACK_CATEGORIES: FeedbackCategory[] = [
  { value: 'BUG', label: '버그 리포트', icon: Bug, color: 'text-red-500', bg: 'bg-red-50' },
  {
    value: 'FEATURE',
    label: '기능 제안',
    icon: Lightbulb,
    color: 'text-amber-500',
    bg: 'bg-amber-50',
  },
  {
    value: 'COMPLAINT',
    label: '불편사항',
    icon: AlertTriangle,
    color: 'text-orange-500',
    bg: 'bg-orange-50',
  },
  { value: 'OTHER', label: '기타', icon: HelpCircle, color: 'text-gray-500', bg: 'bg-gray-100' },
];

const TITLE_MAX = 100;
const CONTENT_MAX = 1000;

/* ------------------------------------------------------------------ */
/*  Main page component                                                */
/* ------------------------------------------------------------------ */

export default function FeedbackPage() {
  /* ---- Form state ---- */
  const [category, setCategory] = useState<string>('');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [email, setEmail] = useState('');

  /* ---- Submission state ---- */
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /* ---- Validation ---- */
  const isValid = category !== '' && title.trim().length > 0 && content.trim().length > 0;

  /* ---- Submit handler ---- */
  const handleSubmit = useCallback(async () => {
    if (!isValid || isSubmitting || isSubmitted) return;

    setIsSubmitting(true);
    setError(null);

    try {
      await apiClient.post('/feedback', {
        category,
        title: title.trim(),
        content: content.trim(),
        email: email.trim() || null,
      });
      setIsSubmitted(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : '제출에 실패했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setIsSubmitting(false);
    }
  }, [category, title, content, email, isValid, isSubmitting, isSubmitted]);

  /* ---- Success view ---- */
  if (isSubmitted) {
    return (
      <div className="mx-auto max-w-screen-xl p-4 md:p-6">
        <Card className="rounded-xl border-0 bg-white shadow-none">
          <CardContent className="flex flex-col items-center gap-4 px-6 py-12 text-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-green-50">
              <CheckCircle2 className="h-7 w-7 text-green-500" />
            </div>
            <h2 className="text-lg font-bold text-gray-900">소중한 피드백 감사합니다!</h2>
            <p className="text-sm text-gray-500">
              보내주신 의견은 서비스 개선에 적극 반영하겠습니다.
              {email && (
                <>
                  <br />
                  답변이 필요한 경우 입력하신 이메일로 안내드리겠습니다.
                </>
              )}
            </p>
            <Link href="/">
              <Button variant="outline" size="sm" className="mt-2">
                <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
                홈으로 돌아가기
              </Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    );
  }

  /* ---- Form view ---- */
  return (
    <div className="mx-auto max-w-screen-xl space-y-4 p-4 md:p-6">
      {/* Page header */}
      <div>
        <h1 className="text-xl font-bold text-gray-900">피드백 보내기</h1>
        <p className="mt-1 text-sm text-gray-500">
          서비스 이용 중 불편한 점이나 개선 아이디어가 있다면 알려주세요.
        </p>
      </div>

      {/* Category selection */}
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="p-4">
          <div className="mb-3 flex items-center gap-2">
            <MessageSquarePlus className="h-4 w-4 text-[#3182F6]" />
            <span className="text-sm font-semibold text-gray-900">피드백 유형</span>
            <span className="text-[10px] text-red-400">*필수</span>
          </div>
          <div className="grid grid-cols-2 gap-2">
            {FEEDBACK_CATEGORIES.map((cat) => {
              const Icon = cat.icon;
              const isSelected = category === cat.value;
              return (
                <button
                  key={cat.value}
                  type="button"
                  onClick={() => setCategory(cat.value)}
                  className={`flex items-center gap-2.5 rounded-lg px-3 py-3 transition-colors ${
                    isSelected
                      ? 'bg-[#3182F6] text-white'
                      : 'bg-gray-50 text-gray-700 hover:bg-gray-100'
                  }`}
                >
                  <Icon
                    className={`h-4 w-4 ${isSelected ? 'text-white' : cat.color}`}
                  />
                  <span className="text-xs font-medium">{cat.label}</span>
                </button>
              );
            })}
          </div>
        </CardContent>
      </Card>

      {/* Form fields */}
      <Card className="rounded-xl border-0 bg-white shadow-none">
        <CardContent className="space-y-4 p-4">
          {/* Title */}
          <div>
            <div className="mb-1 flex items-center justify-between">
              <label htmlFor="fb-title" className="text-xs font-medium text-gray-600">
                제목 <span className="text-red-400">*</span>
              </label>
              <span
                className={`text-[10px] ${
                  title.length > TITLE_MAX ? 'text-red-400' : 'text-gray-300'
                }`}
              >
                {title.length}/{TITLE_MAX}
              </span>
            </div>
            <Input
              id="fb-title"
              type="text"
              maxLength={TITLE_MAX}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="간단히 요약해주세요"
              className="h-10"
            />
          </div>

          {/* Content */}
          <div>
            <div className="mb-1 flex items-center justify-between">
              <label htmlFor="fb-content" className="text-xs font-medium text-gray-600">
                내용 <span className="text-red-400">*</span>
              </label>
              <span
                className={`text-[10px] ${
                  content.length > CONTENT_MAX ? 'text-red-400' : 'text-gray-300'
                }`}
              >
                {content.length}/{CONTENT_MAX}
              </span>
            </div>
            <textarea
              id="fb-content"
              maxLength={CONTENT_MAX}
              rows={6}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="자세히 설명해주시면 더 빠르게 개선할 수 있어요"
              className="w-full resize-none rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-xs outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]"
            />
          </div>

          {/* Email (optional) */}
          <div>
            <label htmlFor="fb-email" className="mb-1 block text-xs font-medium text-gray-600">
              이메일 <span className="text-[10px] text-gray-300">(선택)</span>
            </label>
            <Input
              id="fb-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="답변을 받으실 이메일 (선택사항)"
              className="h-10"
            />
          </div>

          {/* Error message */}
          {error && (
            <div className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-500">{error}</div>
          )}

          {/* Submit button */}
          <Button
            className="w-full bg-[#3182F6] hover:bg-[#2B6FD9] text-white"
            size="lg"
            disabled={!isValid || isSubmitting || isSubmitted}
            onClick={handleSubmit}
          >
            {isSubmitting ? (
              <span className="flex items-center gap-2">
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                제출 중...
              </span>
            ) : (
              <span className="flex items-center gap-2">
                <Send className="h-4 w-4" />
                피드백 보내기
              </span>
            )}
          </Button>
        </CardContent>
      </Card>

      {/* Info note */}
      <p className="px-1 text-[10px] text-gray-400">
        비로그인 상태에서도 피드백을 보낼 수 있습니다. 이메일을 남겨주시면 처리 결과를 안내드립니다.
      </p>
    </div>
  );
}
