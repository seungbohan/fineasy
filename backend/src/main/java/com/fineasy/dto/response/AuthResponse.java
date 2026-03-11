package com.fineasy.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserSummary user
) {

    public record UserSummary(
            long id,
            String email,
            String nickname
    ) {
    }
}
