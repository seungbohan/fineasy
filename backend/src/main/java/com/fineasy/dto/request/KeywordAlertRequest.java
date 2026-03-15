package com.fineasy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KeywordAlertRequest(
        @NotBlank(message = "Keyword must not be blank")
        @Size(min = 1, max = 100, message = "Keyword must be between 1 and 100 characters")
        String keyword
) {
}
