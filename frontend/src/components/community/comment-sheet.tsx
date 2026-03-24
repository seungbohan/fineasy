'use client';

import { useState } from 'react';
import { Send, Trash2, MessageCircle } from 'lucide-react';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/components/ui/sheet';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/stores/auth-store';
import {
  usePostComments,
  useCreateComment,
  useDeleteComment,
} from '@/hooks/use-community';
import { formatRelativeTime } from '@/lib/format';

interface CommentSheetProps {
  postId: number | null;
  stockCode: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/**
 * Bottom sheet for viewing and writing comments on a post.
 * Follows the existing Sheet pattern from the news analysis sheet.
 */
export function CommentSheet({
  postId,
  stockCode,
  open,
  onOpenChange,
}: CommentSheetProps) {
  const { user, isAuthenticated } = useAuthStore();
  const [commentText, setCommentText] = useState('');

  const {
    data,
    isLoading,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = usePostComments(open ? postId : null);

  const createComment = useCreateComment();
  const deleteComment = useDeleteComment();

  const comments = data?.pages.flatMap((p) => p.comments) ?? [];

  const handleSubmit = () => {
    if (!postId || commentText.trim().length < 1 || createComment.isPending)
      return;

    createComment.mutate(
      { postId, content: commentText.trim(), stockCode },
      {
        onSuccess: () => setCommentText(''),
      }
    );
  };

  const handleDelete = (commentId: number) => {
    if (!postId || !confirm('댓글을 삭제하시겠습니까?')) return;
    deleteComment.mutate({ postId, commentId, stockCode });
  };

  return (
    <Sheet
      open={open}
      onOpenChange={(o) => {
        onOpenChange(o);
        if (!o) setCommentText('');
      }}
    >
      <SheetContent
        side="bottom"
        className="mx-auto max-w-screen-md rounded-t-3xl max-h-[85vh] flex flex-col"
        showCloseButton={false}
      >
        {/* Drag handle */}
        <div className="flex justify-center pt-3 pb-1">
          <div className="h-1.5 w-12 rounded-full bg-gray-200" />
        </div>

        <SheetHeader className="px-4 pb-0">
          <SheetTitle className="text-base font-bold text-gray-900 flex items-center gap-2">
            <MessageCircle className="h-4 w-4 text-[#3182F6]" />
            댓글
          </SheetTitle>
          <SheetDescription className="text-[12px] text-gray-400">
            {comments.length > 0
              ? `${comments.length}개의 댓글`
              : '첫 번째 댓글을 남겨보세요'}
          </SheetDescription>
        </SheetHeader>

        {/* Comment list */}
        <div className="flex-1 overflow-y-auto px-4 py-2 space-y-3">
          {isLoading ? (
            <div className="space-y-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="space-y-2">
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-6 w-6 rounded-full" />
                    <Skeleton className="h-3 w-16" />
                    <Skeleton className="h-3 w-12" />
                  </div>
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-2/3" />
                </div>
              ))}
            </div>
          ) : comments.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12">
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100">
                <MessageCircle className="h-5 w-5 text-gray-300" />
              </div>
              <p className="mt-3 text-sm text-gray-400">
                아직 댓글이 없습니다
              </p>
            </div>
          ) : (
            <>
              {comments.map((comment) => (
                <div
                  key={comment.id}
                  className="rounded-xl bg-gray-50 p-3"
                >
                  {comment.isDeleted ? (
                    <p className="text-sm text-gray-400 italic">
                      삭제된 댓글입니다
                    </p>
                  ) : (
                    <>
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <div className="flex h-6 w-6 items-center justify-center rounded-full bg-gray-200">
                            <span className="text-[10px] font-bold text-gray-500">
                              {comment.authorNickname.charAt(0)}
                            </span>
                          </div>
                          <span className="text-[12px] font-semibold text-gray-700">
                            {comment.authorNickname}
                          </span>
                          <span className="text-[11px] text-gray-400">
                            {formatRelativeTime(comment.createdAt)}
                          </span>
                        </div>
                        {user?.nickname === comment.authorNickname && (
                          <button
                            onClick={() => handleDelete(comment.id)}
                            disabled={deleteComment.isPending}
                            className="rounded-lg p-1 text-gray-300 transition-colors hover:text-gray-500"
                            aria-label="댓글 삭제"
                          >
                            <Trash2 className="h-3 w-3" />
                          </button>
                        )}
                      </div>
                      <p className="mt-1.5 text-sm text-gray-700 leading-relaxed whitespace-pre-wrap break-words">
                        {comment.content}
                      </p>
                    </>
                  )}
                </div>
              ))}

              {hasNextPage && (
                <button
                  onClick={() => fetchNextPage()}
                  disabled={isFetchingNextPage}
                  className="w-full rounded-xl py-2.5 text-[13px] font-medium text-gray-500 transition-colors hover:bg-gray-50"
                >
                  {isFetchingNextPage ? '불러오는 중...' : '이전 댓글 더 보기'}
                </button>
              )}
            </>
          )}
        </div>

        {/* Comment input */}
        {isAuthenticated ? (
          <div className="border-t border-gray-100 px-4 py-3">
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSubmit();
                  }
                }}
                placeholder="댓글을 입력하세요"
                maxLength={300}
                className="flex-1 rounded-xl border-0 bg-gray-50 px-3 py-2.5 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-[#3182F6]/30 transition-shadow"
              />
              <Button
                onClick={handleSubmit}
                disabled={
                  commentText.trim().length < 1 || createComment.isPending
                }
                size="sm"
                className="h-9 w-9 shrink-0 rounded-xl bg-[#3182F6] p-0 text-white hover:bg-[#2B6FDB] disabled:opacity-40"
                aria-label="댓글 작성"
              >
                <Send className="h-4 w-4" />
              </Button>
            </div>
          </div>
        ) : (
          <div className="border-t border-gray-100 px-4 py-3 text-center">
            <span className="text-sm text-gray-400">
              댓글을 작성하려면{' '}
              <a
                href="/login"
                className="font-semibold text-[#3182F6] hover:underline"
              >
                로그인
              </a>
              이 필요합니다
            </span>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
