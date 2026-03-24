package com.fineasy.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ReactionRequest(
        @NotNull(message = "Reaction type must not be null")
        @Pattern(regexp = "LIKE|DISLIKE", message = "Reaction type must be LIKE or DISLIKE")
        String reactionType
) {
}
