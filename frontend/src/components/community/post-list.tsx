'use client';

import { useState } from 'react';
import { MessageSquare } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { useStockPosts } from '@/hooks/use-community';
import { PostCard } from './post-card';
import { CommentSheet } from './comment-sheet';

interface PostListProps {
  stockCode: string;
}

/**
 * Paginated post list for the stock community discussion board.
 * Includes loading skeleton, empty state, and infinite scroll via "more" button.
 */
export function PostList({ stockCode }: PostListProps) {
  const {
    data,
    isLoading,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useStockPosts(stockCode);

  const [commentPostId, setCommentPostId] = useState<number | null>(null);
  const [commentSheetOpen, setCommentSheetOpen] = useState(false);

  const posts = data?.pages.flatMap((p) => p.posts) ?? [];

  const handleOpenComments = (postId: number) => {
    setCommentPostId(postId);
    setCommentSheetOpen(true);
  };

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="rounded-2xl bg-white p-4 space-y-3">
            <div className="flex items-center gap-2">
              <Skeleton className="h-7 w-7 rounded-full" />
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-3 w-14" />
            </div>
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-3/4" />
            <div className="flex items-center gap-3 pt-1">
              <Skeleton className="h-7 w-14 rounded-lg" />
              <Skeleton className="h-7 w-14 rounded-lg" />
              <Skeleton className="h-7 w-14 rounded-lg ml-auto" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (posts.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 py-16">
        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-gray-100">
          <MessageSquare className="h-6 w-6 text-gray-300" />
        </div>
        <p className="text-sm font-medium text-gray-500">
          아직 게시글이 없습니다
        </p>
        <p className="text-[12px] text-gray-400">
          첫 번째 글을 작성해보세요!
        </p>
      </div>
    );
  }

  return (
    <>
      <div className="space-y-3">
        {posts.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            stockCode={stockCode}
            onOpenComments={handleOpenComments}
          />
        ))}

        {hasNextPage && (
          <button
            onClick={() => fetchNextPage()}
            disabled={isFetchingNextPage}
            className="w-full rounded-2xl bg-white py-3 text-[13px] font-medium text-gray-500 transition-colors hover:bg-gray-50"
          >
            {isFetchingNextPage ? '불러오는 중...' : '더 보기'}
          </button>
        )}
      </div>

      <CommentSheet
        postId={commentPostId}
        stockCode={stockCode}
        open={commentSheetOpen}
        onOpenChange={(o) => {
          setCommentSheetOpen(o);
          if (!o) setCommentPostId(null);
        }}
      />
    </>
  );
}
