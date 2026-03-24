'use client';

import {
  useInfiniteQuery,
  useQuery,
  useMutation,
  useQueryClient,
} from '@tanstack/react-query';
import {
  PostListData,
  Post,
  ReactionResponse,
  CommentListData,
} from '@/types';
import { apiClient } from '@/lib/api-client';

const POST_PAGE_SIZE = 20;
const COMMENT_PAGE_SIZE = 30;

/**
 * Fetch paginated posts for a stock discussion board.
 * Uses cursor-based infinite scrolling.
 */
export function useStockPosts(stockCode: string) {
  return useInfiniteQuery<PostListData>({
    queryKey: ['community', stockCode, 'posts'],
    queryFn: async ({ pageParam }) => {
      const params = new URLSearchParams();
      params.set('size', String(POST_PAGE_SIZE));
      if (pageParam) params.set('cursor', String(pageParam));
      return apiClient.get<PostListData>(
        `/stocks/${stockCode}/posts?${params.toString()}`
      );
    },
    initialPageParam: null as number | null,
    getNextPageParam: (lastPage) =>
      lastPage.hasNext ? lastPage.nextCursor : undefined,
    enabled: !!stockCode,
  });
}

/**
 * Fetch the total post count for a stock discussion board.
 */
export function usePostCount(stockCode: string) {
  return useQuery<{ count: number }>({
    queryKey: ['community', stockCode, 'postCount'],
    queryFn: () =>
      apiClient.get<{ count: number }>(
        `/stocks/${stockCode}/posts/count`
      ),
    enabled: !!stockCode,
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Create a new post in a stock discussion board.
 * Invalidates post list and count on success.
 */
export function useCreatePost() {
  const queryClient = useQueryClient();

  return useMutation<Post, Error, { stockCode: string; content: string }>({
    mutationFn: ({ stockCode, content }) =>
      apiClient.post<Post>(`/stocks/${stockCode}/posts`, { content }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['community', variables.stockCode, 'posts'],
      });
      queryClient.invalidateQueries({
        queryKey: ['community', variables.stockCode, 'postCount'],
      });
    },
  });
}

/**
 * Delete a post from a stock discussion board.
 * Invalidates post list and count on success.
 */
export function useDeletePost() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, { stockCode: string; postId: number }>({
    mutationFn: ({ stockCode, postId }) =>
      apiClient.delete<void>(`/stocks/${stockCode}/posts/${postId}`),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['community', variables.stockCode, 'posts'],
      });
      queryClient.invalidateQueries({
        queryKey: ['community', variables.stockCode, 'postCount'],
      });
    },
  });
}

/**
 * Toggle a reaction (LIKE/DISLIKE) on a post.
 * Uses optimistic update for instant UI feedback.
 */
export function useToggleReaction(stockCode: string) {
  const queryClient = useQueryClient();

  return useMutation<
    ReactionResponse,
    Error,
    { postId: number; reactionType: 'LIKE' | 'DISLIKE' }
  >({
    mutationFn: ({ postId, reactionType }) =>
      apiClient.post<ReactionResponse>(`/posts/${postId}/reactions`, {
        reactionType,
      }),
    onMutate: async ({ postId, reactionType }) => {
      // Cancel ongoing fetches
      await queryClient.cancelQueries({
        queryKey: ['community', stockCode, 'posts'],
      });

      type InfinitePostData = {
        pages: PostListData[];
        pageParams: (number | null)[];
      };

      // Snapshot previous data for rollback
      const previousData = queryClient.getQueryData<InfinitePostData>(
        ['community', stockCode, 'posts']
      );

      // Optimistic update
      queryClient.setQueryData<InfinitePostData>(
        ['community', stockCode, 'posts'],
        (old) => {
          if (!old) return old;
          return {
            ...old,
            pages: old.pages.map((page) => ({
              ...page,
              posts: page.posts.map((post) => {
                if (post.id !== postId) return post;

                const wasActive = post.myReaction === reactionType;
                const wasOpposite =
                  post.myReaction !== null && post.myReaction !== reactionType;

                return {
                  ...post,
                  myReaction: wasActive ? null : reactionType,
                  likeCount:
                    reactionType === 'LIKE'
                      ? post.likeCount + (wasActive ? -1 : 1)
                      : post.likeCount -
                        (wasOpposite && post.myReaction === 'LIKE' ? 1 : 0),
                  dislikeCount:
                    reactionType === 'DISLIKE'
                      ? post.dislikeCount + (wasActive ? -1 : 1)
                      : post.dislikeCount -
                        (wasOpposite && post.myReaction === 'DISLIKE' ? 1 : 0),
                };
              }),
            })),
          };
        }
      );

      return { previousData };
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onError: (_err: Error, _variables: any, context: any) => {
      // Rollback on error
      if (context?.previousData) {
        queryClient.setQueryData(
          ['community', stockCode, 'posts'],
          context.previousData
        );
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({
        queryKey: ['community', stockCode, 'posts'],
      });
    },
  });
}

/**
 * Fetch paginated comments for a post.
 * Uses cursor-based infinite scrolling.
 */
export function usePostComments(postId: number | null) {
  return useInfiniteQuery<CommentListData>({
    queryKey: ['community', 'comments', postId],
    queryFn: async ({ pageParam }) => {
      const params = new URLSearchParams();
      params.set('size', String(COMMENT_PAGE_SIZE));
      if (pageParam) params.set('cursor', String(pageParam));
      return apiClient.get<CommentListData>(
        `/posts/${postId}/comments?${params.toString()}`
      );
    },
    initialPageParam: null as number | null,
    getNextPageParam: (lastPage) =>
      lastPage.hasNext ? lastPage.nextCursor : undefined,
    enabled: postId !== null,
  });
}

/**
 * Create a new comment on a post.
 * Invalidates comment list and post data on success.
 */
export function useCreateComment() {
  const queryClient = useQueryClient();

  return useMutation<
    Comment,
    Error,
    { postId: number; content: string; stockCode: string }
  >({
    mutationFn: ({ postId, content }) =>
      apiClient.post<Comment>(`/posts/${postId}/comments`, { content }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['community', 'comments', variables.postId],
      });
      queryClient.invalidateQueries({
        queryKey: ['community', variables.stockCode, 'posts'],
      });
    },
  });
}

/**
 * Delete a comment on a post.
 * Invalidates comment list and post data on success.
 */
export function useDeleteComment() {
  const queryClient = useQueryClient();

  return useMutation<
    void,
    Error,
    { postId: number; commentId: number; stockCode: string }
  >({
    mutationFn: ({ postId, commentId }) =>
      apiClient.delete<void>(`/posts/${postId}/comments/${commentId}`),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['community', 'comments', variables.postId],
      });
      queryClient.invalidateQueries({
        queryKey: ['community', variables.stockCode, 'posts'],
      });
    },
  });
}
