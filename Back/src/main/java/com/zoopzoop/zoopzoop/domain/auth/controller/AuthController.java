package com.zoopzoop.zoopzoop.domain.auth.controller;

import com.zoopzoop.zoopzoop.domain.auth.dto.request.LoginRequest;
import com.zoopzoop.zoopzoop.domain.auth.dto.request.PasswordResetRequest;
import com.zoopzoop.zoopzoop.domain.auth.dto.request.SignupRequest;
import com.zoopzoop.zoopzoop.domain.auth.dto.response.AuthResponse;
import com.zoopzoop.zoopzoop.domain.auth.dto.response.OAuthRedirectResponse;
import com.zoopzoop.zoopzoop.domain.auth.dto.response.PasswordResetResponse;
import com.zoopzoop.zoopzoop.domain.auth.service.AuthResult;
import com.zoopzoop.zoopzoop.domain.auth.service.AuthService;
import com.zoopzoop.zoopzoop.domain.auth.service.GoogleOAuthService;
import com.zoopzoop.zoopzoop.domain.auth.service.RefreshTokenIssue;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;
    private final String refreshTokenCookieName;
    private final boolean refreshTokenCookieSecure;
    private final String refreshTokenCookieSameSite;

    public AuthController(
            AuthService authService,
            GoogleOAuthService googleOAuthService,
            @Value("${jwt.refresh-token-cookie-name}") String refreshTokenCookieName,
            @Value("${jwt.refresh-token-cookie-secure}") boolean refreshTokenCookieSecure,
            @Value("${jwt.refresh-token-cookie-same-site}") String refreshTokenCookieSameSite
    ) {
        this.authService = authService;
        this.googleOAuthService = googleOAuthService;
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.refreshTokenCookieSecure = refreshTokenCookieSecure;
        this.refreshTokenCookieSameSite = refreshTokenCookieSameSite;
    }

    @PostMapping("/signup")
    public ApiResponse<AuthResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletResponse response
    ) {
        AuthResult result = authService.signup(request);
        setRefreshTokenCookie(response, result.refreshToken());
        return ApiResponse.ok(result.response());
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResult result = authService.login(request);
        setRefreshTokenCookie(response, result.refreshToken());
        return ApiResponse.ok(result.response());
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
            @CookieValue(name = "${jwt.refresh-token-cookie-name}", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        AuthResult result = authService.refresh(refreshToken);
        setRefreshTokenCookie(response, result.refreshToken());
        return ApiResponse.ok(result.response());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = "${jwt.refresh-token-cookie-name}", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        clearRefreshTokenCookie(response);
        return ApiResponse.ok(null);
    }

    @PostMapping("/password-reset")
    public ApiResponse<PasswordResetResponse> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        return ApiResponse.ok(authService.resetPassword(request));
    }

    @GetMapping("/google")
    public ResponseEntity<Void> redirectToGoogle() {
        return ResponseEntity.status(302)
                .location(URI.create(googleOAuthService.buildAuthorizationUrl()))
                .build();
    }

    @GetMapping("/google/url")
    public ApiResponse<OAuthRedirectResponse> getGoogleLoginUrl() {
        return ApiResponse.ok(new OAuthRedirectResponse(googleOAuthService.buildAuthorizationUrl()));
    }

    @GetMapping("/google/callback")
    public ApiResponse<AuthResponse> googleCallback(
            @RequestParam String code,
            HttpServletResponse response
    ) {
        AuthResult result = googleOAuthService.loginWithCode(code);
        setRefreshTokenCookie(response, result.refreshToken());
        return ApiResponse.ok(result.response());
    }

    @GetMapping("/health")
    public ApiResponse<HealthCheckDto> health() {
        return ApiResponse.ok(authService.getStatus());
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

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
