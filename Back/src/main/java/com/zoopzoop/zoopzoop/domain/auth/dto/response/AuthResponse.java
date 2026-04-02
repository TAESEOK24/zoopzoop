package com.zoopzoop.zoopzoop.domain.auth.dto.response;

import com.zoopzoop.zoopzoop.domain.user.dto.UserSummary;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserSummary user
) {
    public static AuthResponse of(String accessToken, UserSummary user) {
        return new AuthResponse(accessToken, "Bearer", user);
    }
}
