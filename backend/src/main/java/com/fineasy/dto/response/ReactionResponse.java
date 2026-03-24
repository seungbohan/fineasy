package com.fineasy.dto.response;

public record ReactionResponse(
        Long postId,
        int likeCount,
        int dislikeCount,
        String myReaction
) {
}
