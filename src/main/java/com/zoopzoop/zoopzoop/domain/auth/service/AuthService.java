package com.zoopzoop.zoopzoop.domain.auth.service;

import com.zoopzoop.zoopzoop.global.security.jwt.JwtProvider;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtProvider jwtProvider;

    public AuthService(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("auth", jwtProvider.issuePlaceholderToken());
    }
}
