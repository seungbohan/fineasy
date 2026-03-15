'use client';

import { useState } from 'react';
import { X, Plus, Bell } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  useAlertKeywords,
  useAddKeyword,
  useDeleteKeyword,
} from '@/hooks/use-alert-keywords';

/**
 * Alert keyword management UI.
 * Allows users to add/remove news alert keywords (max 10).
 */
export function AlertKeywords() {
  const { data, isLoading } = useAlertKeywords();
  const addKeyword = useAddKeyword();
  const deleteKeyword = useDeleteKeyword();
  const [input, setInput] = useState('');

  const keywords = data?.keywords ?? [];
  const maxCount = data?.maxCount ?? 10;
  const isFull = keywords.length >= maxCount;

  const handleAdd = () => {
    const trimmed = input.trim();
    if (!trimmed) return;
    if (keywords.some((k) => k.keyword === trimmed)) return;
    if (isFull) return;

    addKeyword.mutate(trimmed, {
      onSuccess: () => setInput(''),
    });
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAdd();
    }
  };

  if (isLoading) {
    return (
      <Card className="rounded-2xl border-0 bg-white shadow-none">
        <CardContent className="p-4 space-y-3">
          <Skeleton className="h-5 w-32" />
          <div className="flex gap-2">
            <Skeleton className="h-8 w-20 rounded-full" />
            <Skeleton className="h-8 w-24 rounded-full" />
            <Skeleton className="h-8 w-16 rounded-full" />
          </div>
          <Skeleton className="h-9 w-full rounded-lg" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="rounded-2xl border-0 bg-white shadow-none">
      <CardContent className="p-4 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Bell className="h-4 w-4 text-[#3182F6]" />
            <h3 className="text-[15px] font-bold text-gray-900">
              알림 키워드
            </h3>
          </div>
          <span className="text-[11px] text-gray-400 tabular-nums">
            {keywords.length}/{maxCount}
          </span>
        </div>

        {keywords.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {keywords.map((kw) => (
              <span
                key={kw.id}
                className="inline-flex items-center gap-1 rounded-full bg-blue-50 px-3 py-1.5 text-xs font-medium text-[#3182F6] transition-colors hover:bg-blue-100"
              >
                {kw.keyword}
                <button
                  onClick={() => deleteKeyword.mutate(kw.id)}
                  className="flex h-4 w-4 items-center justify-center rounded-full text-blue-400 hover:bg-blue-200 hover:text-blue-600 transition-colors"
                  aria-label={`${kw.keyword} 삭제`}
                  disabled={deleteKeyword.isPending}
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            ))}
          </div>
        ) : (
          <p className="text-[13px] text-gray-400 py-2">
            관심 키워드를 등록하면 관련 뉴스 알림을 받을 수 있어요
          </p>
        )}

        <div className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={isFull ? '최대 등록 수 도달' : '키워드 입력'}
            disabled={isFull}
            className="flex-1 rounded-xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-900 placeholder:text-gray-400 outline-none focus:border-[#3182F6] focus:ring-1 focus:ring-[#3182F6]/20 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            maxLength={20}
          />
          <button
            onClick={handleAdd}
            disabled={!input.trim() || isFull || addKeyword.isPending}
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[#3182F6] text-white transition-all hover:bg-[#1B6BF3] active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed"
            aria-label="키워드 추가"
          >
            <Plus className="h-4 w-4" />
          </button>
        </div>
      </CardContent>
    </Card>
  );
}
