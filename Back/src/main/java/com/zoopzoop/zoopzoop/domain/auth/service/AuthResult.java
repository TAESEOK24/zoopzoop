package com.zoopzoop.zoopzoop.domain.auth.service;

import com.zoopzoop.zoopzoop.domain.auth.dto.response.AuthResponse;

public record AuthResult(
        AuthResponse response,
        RefreshTokenIssue refreshToken
) {
}
