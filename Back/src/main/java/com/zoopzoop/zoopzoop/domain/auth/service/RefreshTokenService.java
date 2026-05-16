package com.zoopzoop.zoopzoop.domain.auth.service;

import com.zoopzoop.zoopzoop.domain.auth.entity.RefreshToken;
import com.zoopzoop.zoopzoop.domain.auth.repository.RefreshTokenRepository;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationSeconds;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token-expiration-seconds}") long refreshTokenExpirationSeconds
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    @Transactional
    public RefreshTokenIssue issue(User user) {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        String token = generateOpaqueToken();
        String tokenHash = hash(token);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationSeconds))
                .build());

        return new RefreshTokenIssue(token, refreshTokenExpirationSeconds);
    }

    @Transactional
    public RefreshTokenIssue rotate(String currentToken) {
        RefreshToken currentRefreshToken = findUsableToken(currentToken);
        RefreshTokenIssue nextToken = issue(currentRefreshToken.getUser());
        currentRefreshToken.revoke(hash(nextToken.token()));
        return nextToken;
    }

    @Transactional(readOnly = true)
    public User getUserFromUsableToken(String token) {
        return findUsableToken(token).getUser();
    }

    @Transactional
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hash(token))
                .filter(refreshToken -> refreshToken.getRevokedAt() == null)
                .ifPresent(refreshToken -> refreshToken.revoke(null));
    }

    private RefreshToken findUsableToken(String token) {
        if (token == null || token.isBlank()) {
            throw new AppException(401, "Refresh token is missing.");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new AppException(401, "Refresh token is invalid."));

        if (!refreshToken.isUsable(LocalDateTime.now())) {
            throw new AppException(401, "Refresh token is expired or revoked.");
        }

        return refreshToken;
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }
}
