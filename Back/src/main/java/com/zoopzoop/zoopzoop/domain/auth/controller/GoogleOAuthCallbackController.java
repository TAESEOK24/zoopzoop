package com.zoopzoop.zoopzoop.domain.auth.controller;

import com.zoopzoop.zoopzoop.domain.auth.dto.response.AuthResponse;
import com.zoopzoop.zoopzoop.domain.auth.service.AuthResult;
import com.zoopzoop.zoopzoop.domain.auth.service.GoogleOAuthService;
import com.zoopzoop.zoopzoop.domain.auth.service.RefreshTokenIssue;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoogleOAuthCallbackController {

    private final GoogleOAuthService googleOAuthService;
    private final String refreshTokenCookieName;
    private final boolean refreshTokenCookieSecure;
    private final String refreshTokenCookieSameSite;

    public GoogleOAuthCallbackController(
            GoogleOAuthService googleOAuthService,
            @Value("${jwt.refresh-token-cookie-name}") String refreshTokenCookieName,
            @Value("${jwt.refresh-token-cookie-secure}") boolean refreshTokenCookieSecure,
            @Value("${jwt.refresh-token-cookie-same-site}") String refreshTokenCookieSameSite
    ) {
        this.googleOAuthService = googleOAuthService;
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.refreshTokenCookieSecure = refreshTokenCookieSecure;
        this.refreshTokenCookieSameSite = refreshTokenCookieSameSite;
    }

    @GetMapping("/login/oauth2/code/google")
    public ApiResponse<AuthResponse> googleCallback(@RequestParam String code, HttpServletResponse response) {
        AuthResult result = googleOAuthService.loginWithCode(code);
        setRefreshTokenCookie(response, result.refreshToken());
        return ApiResponse.ok(result.response());
    }

    private void setRefreshTokenCookie(HttpServletResponse response, RefreshTokenIssue refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenCookieName, refreshToken.token())
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(refreshToken.maxAgeSeconds()))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
