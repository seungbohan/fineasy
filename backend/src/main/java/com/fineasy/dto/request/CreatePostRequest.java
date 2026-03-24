package com.fineasy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank(message = "Content must not be blank")
        @Size(min = 10, max = 500, message = "Content must be between 10 and 500 characters")
        String content
) {
}
