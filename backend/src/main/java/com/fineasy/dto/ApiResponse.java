package com.fineasy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Map<String, Object> meta
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null,
                Map.of("timestamp", Instant.now().toString()));
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null,
                new ApiError(code, message),
                Map.of("timestamp", Instant.now().toString()));
    }

    public record ApiError(String code, String message) {
    }
}
