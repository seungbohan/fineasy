'use client';

import { ThumbsUp, ThumbsDown, MessageCircle, Trash2 } from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { useToggleReaction, useDeletePost } from '@/hooks/use-community';
import { formatRelativeTime } from '@/lib/format';
import type { Post } from '@/types';

interface PostCardProps {
  post: Post;
  stockCode: string;
  onOpenComments: (postId: number) => void;
}

/**
 * Individual post card for the stock community discussion board.
 * Displays author info, content, reactions, and comment count.
 */
export function PostCard({ post, stockCode, onOpenComments }: PostCardProps) {
  const { user } = useAuthStore();
  const toggleReaction = useToggleReaction(stockCode);
  const deletePost = useDeletePost();

  const isAuthor = user?.nickname === post.authorNickname;

  const handleReaction = (reactionType: 'LIKE' | 'DISLIKE') => {
    if (!user) return;
    toggleReaction.mutate({ postId: post.id, reactionType });
  };

  const handleDelete = () => {
    if (!confirm('게시글을 삭제하시겠습니까?')) return;
    deletePost.mutate({ stockCode, postId: post.id });
  };

  if (post.isDeleted) {
    return (
      <div className="rounded-2xl bg-white p-4">
        <p className="text-sm text-gray-400 italic">삭제된 게시글입니다</p>
      </div>
    );
  }

  return (
    <div className="rounded-2xl bg-white p-4">
      {/* Header: nickname + time */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-full bg-gray-100">
            <span className="text-[11px] font-bold text-gray-500">
              {post.authorNickname.charAt(0)}
            </span>
          </div>
          <span className="text-[13px] font-semibold text-gray-900">
            {post.authorNickname}
          </span>
          <span className="text-[11px] text-gray-400">
            {formatRelativeTime(post.createdAt)}
          </span>
        </div>
        {isAuthor && (
          <button
            onClick={handleDelete}
            disabled={deletePost.isPending}
            className="rounded-lg p-1.5 text-gray-300 transition-colors hover:bg-gray-50 hover:text-gray-500"
            aria-label="게시글 삭제"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        )}
      </div>

      {/* Content */}
      <p className="mt-2.5 text-sm text-gray-800 leading-relaxed whitespace-pre-wrap break-words">
        {post.content}
      </p>

      {/* Footer: reactions + comments */}
      <div className="mt-3 flex items-center gap-1">
        <button
          onClick={() => handleReaction('LIKE')}
          className={`flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-[12px] font-medium transition-colors ${
            post.myReaction === 'LIKE'
              ? 'bg-blue-50 text-blue-500'
              : 'text-gray-400 hover:bg-gray-50 hover:text-gray-600'
          }`}
          aria-label="좋아요"
        >
          <ThumbsUp className="h-3.5 w-3.5" />
          <span className="tabular-nums">{post.likeCount}</span>
        </button>

        <button
          onClick={() => handleReaction('DISLIKE')}
          className={`flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-[12px] font-medium transition-colors ${
            post.myReaction === 'DISLIKE'
              ? 'bg-red-50 text-red-500'
              : 'text-gray-400 hover:bg-gray-50 hover:text-gray-600'
          }`}
          aria-label="싫어요"
        >
          <ThumbsDown className="h-3.5 w-3.5" />
          <span className="tabular-nums">{post.dislikeCount}</span>
        </button>

        <button
          onClick={() => onOpenComments(post.id)}
          className="ml-auto flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-[12px] font-medium text-gray-400 transition-colors hover:bg-gray-50 hover:text-gray-600"
          aria-label="댓글 보기"
        >
          <MessageCircle className="h-3.5 w-3.5" />
          <span className="tabular-nums">{post.commentCount}</span>
        </button>
      </div>
    </div>
  );
}
