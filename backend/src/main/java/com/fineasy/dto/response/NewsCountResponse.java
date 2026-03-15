package com.fineasy.dto.response;

import java.time.LocalDateTime;

public record NewsCountResponse(
        long count,
        LocalDateTime since
) {
}
