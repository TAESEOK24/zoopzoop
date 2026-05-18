package com.zoopzoop.zoopzoop.domain.auth.service;

public record RefreshTokenIssue(
        String token,
        long maxAgeSeconds
) {
}
