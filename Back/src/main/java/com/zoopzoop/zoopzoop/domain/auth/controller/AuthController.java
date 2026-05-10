package com.zoopzoop.zoopzoop.domain.auth.controller;

import com.zoopzoop.zoopzoop.domain.auth.dto.request.LoginRequest;
import com.zoopzoop.zoopzoop.domain.auth.dto.request.SignupRequest;
import com.zoopzoop.zoopzoop.domain.auth.dto.response.AuthResponse;
import com.zoopzoop.zoopzoop.domain.auth.dto.response.OAuthRedirectResponse;
import com.zoopzoop.zoopzoop.domain.auth.service.AuthService;
import com.zoopzoop.zoopzoop.domain.auth.service.GoogleOAuthService;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
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

    public AuthController(AuthService authService, GoogleOAuthService googleOAuthService) {
        this.authService = authService;
        this.googleOAuthService = googleOAuthService;
    }

    @PostMapping("/signup")
    public ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
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
    public ApiResponse<AuthResponse> googleCallback(@RequestParam String code) {
        return ApiResponse.ok(googleOAuthService.loginWithCode(code));
    }

    @GetMapping("/health")
    public ApiResponse<HealthCheckDto> health() {
        return ApiResponse.ok(authService.getStatus());
    }
}
