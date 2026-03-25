'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Send } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/stores/auth-store';
import { useCreatePost } from '@/hooks/use-community';

const MIN_LENGTH = 1;
const MAX_LENGTH = 500;

interface PostWriteFormProps {
  stockCode: string;
}

/**
 * Post creation form for the stock community discussion board.
 * Shows a fake textarea for unauthenticated users that redirects to login on tap.
 */
export function PostWriteForm({ stockCode }: PostWriteFormProps) {
  const { isAuthenticated } = useAuthStore();
  const [content, setContent] = useState('');
  const createPost = useCreatePost();
  const router = useRouter();

  const trimmedLength = content.trim().length;
  const isValid = trimmedLength >= MIN_LENGTH && trimmedLength <= MAX_LENGTH;

  const handleSubmit = () => {
    if (!isValid || createPost.isPending) return;

    createPost.mutate(
      { stockCode, content: content.trim() },
      {
        onSuccess: () => setContent(''),
      }
    );
  };

  if (!isAuthenticated) {
    return (
      <div className="rounded-2xl bg-white p-4">
        <button
          onClick={() => {
            if (confirm('글을 작성하려면 로그인이 필요합니다.\n로그인 하시겠습니까?')) {
              router.push('/login');
            }
          }}
          className="w-full text-left rounded-xl bg-gray-50 p-3 text-sm text-gray-400 cursor-pointer transition-all hover:ring-2 hover:ring-[#3182F6]/30"
        >
          종목에 대한 의견을 공유해보세요
        </button>
      </div>
    );
  }

  return (
    <div className="rounded-2xl bg-white p-4">
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="종목에 대한 의견을 공유해보세요"
        maxLength={MAX_LENGTH}
        rows={3}
        className="w-full resize-none rounded-xl border-0 bg-gray-50 p-3 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-[#3182F6]/30 transition-shadow"
      />
      <div className="mt-2 flex items-center justify-between">
        <span
          className={`text-[11px] tabular-nums ${
            trimmedLength > MAX_LENGTH
              ? 'text-red-500'
              : trimmedLength >= MIN_LENGTH
                ? 'text-gray-400'
                : 'text-gray-300'
          }`}
        >
          {trimmedLength}/{MAX_LENGTH}
        </span>
        <Button
          onClick={handleSubmit}
          disabled={!isValid || createPost.isPending}
          size="sm"
          className="h-8 rounded-lg bg-[#3182F6] px-4 text-[13px] font-semibold text-white hover:bg-[#2B6FDB] disabled:opacity-40"
        >
          <Send className="mr-1.5 h-3.5 w-3.5" />
          {createPost.isPending ? '작성 중...' : '작성'}
        </Button>
      </div>
    </div>
  );
}
