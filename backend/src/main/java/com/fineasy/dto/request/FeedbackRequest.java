package com.fineasy.dto.request;

import com.fineasy.entity.FeedbackType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @NotNull(message = "Feedback type is required")
        FeedbackType type,

        @NotBlank(message = "Title must not be blank")
        @Size(max = 100, message = "Title must not exceed 100 characters")
        String title,

        @NotBlank(message = "Content must not be blank")
        @Size(max = 1000, message = "Content must not exceed 1000 characters")
        String content,

        @Email(message = "Invalid email format")
        String contactEmail
) {
}
