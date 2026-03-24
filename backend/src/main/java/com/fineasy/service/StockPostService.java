package com.fineasy.service;

import com.fineasy.dto.response.*;
import com.fineasy.entity.*;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.exception.ForbiddenException;
import com.fineasy.exception.ProfanityException;
import com.fineasy.repository.*;
import com.fineasy.util.HtmlSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StockPostService {

    private static final Logger log = LoggerFactory.getLogger(StockPostService.class);
    private static final String DELETED_CONTENT_PLACEHOLDER = "삭제된 게시글입니다";
    private static final String DELETED_COMMENT_PLACEHOLDER = "삭제된 댓글입니다";

    private final StockPostRepository postRepository;
    private final StockPostReactionRepository reactionRepository;
    private final StockPostCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ProfanityFilter profanityFilter;

    public StockPostService(StockPostRepository postRepository,
                            StockPostReactionRepository reactionRepository,
                            StockPostCommentRepository commentRepository,
                            UserRepository userRepository,
                            ProfanityFilter profanityFilter) {
        this.postRepository = postRepository;
        this.reactionRepository = reactionRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.profanityFilter = profanityFilter;
    }

    // ── Posts ──

    public StockPostListResponse getPosts(String stockCode, Long cursor, int size, Long currentUserId) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<StockPostEntity> posts;
        if (cursor != null) {
            posts = postRepository.findByStockCodeWithCursor(stockCode, cursor, pageRequest);
        } else {
            posts = postRepository.findByStockCodeLatest(stockCode, pageRequest);
        }

        boolean hasNext = posts.size() > size;
        if (hasNext) {
            posts = posts.subList(0, size);
        }

        Map<Long, ReactionType> myReactions = fetchMyReactions(posts, currentUserId);

        List<StockPostResponse> postResponses = posts.stream()
                .map(post -> toPostResponse(post, myReactions.get(post.getId())))
                .toList();

        Long nextCursor = hasNext && !posts.isEmpty()
                ? posts.get(posts.size() - 1).getId()
                : null;

        return new StockPostListResponse(postResponses, nextCursor, hasNext);
    }

    @Transactional
    public StockPostResponse createPost(String stockCode, String content, long userId) {
        if (profanityFilter.containsProfanity(content)) {
            throw new ProfanityException();
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND", "User not found"));

        String sanitizedContent = HtmlSanitizer.escape(content);
        StockPostEntity post = new StockPostEntity(stockCode, user, sanitizedContent);
        StockPostEntity saved = postRepository.save(post);

        log.info("Post created: id={}, stockCode={}, userId={}", saved.getId(), stockCode, userId);
        return toPostResponse(saved, null);
    }

    @Transactional
    public void deletePost(Long postId, long userId) {
        StockPostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("POST_NOT_FOUND", "Post not found"));

        if (post.getUser().getId() != userId) {
            throw new ForbiddenException("You can only delete your own posts");
        }

        post.softDelete();
        log.info("Post soft-deleted: id={}, userId={}", postId, userId);
    }

    public PostCountResponse getPostCount(String stockCode) {
        long count = postRepository.countByStockCode(stockCode);
        return new PostCountResponse(stockCode, count);
    }

    // ── Reactions ──

    @Transactional
    public ReactionResponse toggleReaction(Long postId, String reactionTypeStr, long userId) {
        ReactionType requestedType = ReactionType.valueOf(reactionTypeStr);

        StockPostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("POST_NOT_FOUND", "Post not found"));

        var existingReaction = reactionRepository.findByPostIdAndUserId(postId, userId);

        String myReaction;

        if (existingReaction.isPresent()) {
            StockPostReactionEntity reaction = existingReaction.get();

            if (reaction.getReactionType() == requestedType) {
                // Same reaction: cancel it
                reactionRepository.delete(reaction);
                updateReactionCount(postId, requestedType, -1);
                myReaction = null;
            } else {
                // Different reaction: switch
                ReactionType oldType = reaction.getReactionType();
                reaction.changeReactionType(requestedType);
                reactionRepository.save(reaction);
                updateReactionCount(postId, oldType, -1);
                updateReactionCount(postId, requestedType, 1);
                myReaction = requestedType.name();
            }
        } else {
            // New reaction
            StockPostReactionEntity newReaction =
                    new StockPostReactionEntity(post, userId, requestedType);
            reactionRepository.save(newReaction);
            updateReactionCount(postId, requestedType, 1);
            myReaction = requestedType.name();
        }

        // Flush to ensure atomic UPDATE is applied, then re-read
        postRepository.flush();
        StockPostEntity refreshed = postRepository.findById(postId).orElse(post);

        return new ReactionResponse(
                postId,
                refreshed.getLikeCount(),
                refreshed.getDislikeCount(),
                myReaction
        );
    }

    // ── Comments ──

    public StockPostCommentListResponse getComments(Long postId, Long cursor, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<StockPostCommentEntity> comments;
        if (cursor != null) {
            comments = commentRepository.findByPostIdWithCursor(postId, cursor, pageRequest);
        } else {
            comments = commentRepository.findByPostIdOldest(postId, pageRequest);
        }

        boolean hasNext = comments.size() > size;
        if (hasNext) {
            comments = comments.subList(0, size);
        }

        List<StockPostCommentResponse> commentResponses = comments.stream()
                .map(this::toCommentResponse)
                .toList();

        Long nextCursor = hasNext && !comments.isEmpty()
                ? comments.get(comments.size() - 1).getId()
                : null;

        return new StockPostCommentListResponse(commentResponses, nextCursor, hasNext);
    }

    @Transactional
    public StockPostCommentResponse createComment(Long postId, String content, long userId) {
        if (profanityFilter.containsProfanity(content)) {
            throw new ProfanityException();
        }

        StockPostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("POST_NOT_FOUND", "Post not found"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND", "User not found"));

        String sanitizedContent = HtmlSanitizer.escape(content);
        StockPostCommentEntity comment = new StockPostCommentEntity(post, user, sanitizedContent);
        StockPostCommentEntity saved = commentRepository.save(comment);

        post.incrementCommentCount();

        log.info("Comment created: id={}, postId={}, userId={}", saved.getId(), postId, userId);
        return toCommentResponse(saved);
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, long userId) {
        StockPostCommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("COMMENT_NOT_FOUND", "Comment not found"));

        if (!comment.getPost().getId().equals(postId)) {
            throw new EntityNotFoundException("COMMENT_NOT_FOUND", "Comment not found in this post");
        }

        if (comment.getUser().getId() != userId) {
            throw new ForbiddenException("You can only delete your own comments");
        }

        comment.softDelete();
        comment.getPost().decrementCommentCount();

        log.info("Comment soft-deleted: id={}, postId={}, userId={}", commentId, postId, userId);
    }

    // ── Private helpers ──

    private void updateReactionCount(Long postId, ReactionType type, int delta) {
        if (type == ReactionType.LIKE) {
            postRepository.updateLikeCount(postId, delta);
        } else {
            postRepository.updateDislikeCount(postId, delta);
        }
    }

    private Map<Long, ReactionType> fetchMyReactions(List<StockPostEntity> posts, Long currentUserId) {
        if (currentUserId == null || posts.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> postIds = posts.stream().map(StockPostEntity::getId).toList();
        return reactionRepository.findByPostIdInAndUserId(postIds, currentUserId)
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getPost().getId(),
                        StockPostReactionEntity::getReactionType
                ));
    }

    private StockPostResponse toPostResponse(StockPostEntity post, ReactionType myReaction) {
        String displayContent = post.isDeleted()
                ? DELETED_CONTENT_PLACEHOLDER
                : post.getContent();

        return new StockPostResponse(
                post.getId(),
                displayContent,
                post.getUser().getNickname(),
                post.getLikeCount(),
                post.getDislikeCount(),
                post.getCommentCount(),
                myReaction != null ? myReaction.name() : null,
                post.isDeleted(),
                post.getCreatedAt()
        );
    }

    private StockPostCommentResponse toCommentResponse(StockPostCommentEntity comment) {
        String displayContent = comment.isDeleted()
                ? DELETED_COMMENT_PLACEHOLDER
                : comment.getContent();

        return new StockPostCommentResponse(
                comment.getId(),
                displayContent,
                comment.getUser().getNickname(),
                comment.isDeleted(),
                comment.getCreatedAt()
        );
    }
}
