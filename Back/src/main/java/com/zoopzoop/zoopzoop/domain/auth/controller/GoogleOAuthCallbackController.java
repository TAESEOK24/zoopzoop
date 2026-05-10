package com.zoopzoop.zoopzoop.domain.auth.controller;

import com.zoopzoop.zoopzoop.domain.auth.dto.response.AuthResponse;
import com.zoopzoop.zoopzoop.domain.auth.service.GoogleOAuthService;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoogleOAuthCallbackController {

    private final GoogleOAuthService googleOAuthService;

    public GoogleOAuthCallbackController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    @GetMapping("/login/oauth2/code/google")
    public ApiResponse<AuthResponse> googleCallback(@RequestParam String code) {
        return ApiResponse.ok(googleOAuthService.loginWithCode(code));
    }
}
