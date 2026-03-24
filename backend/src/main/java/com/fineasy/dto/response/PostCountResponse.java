package com.fineasy.dto.response;

public record PostCountResponse(
        String stockCode,
        long count
) {
}
