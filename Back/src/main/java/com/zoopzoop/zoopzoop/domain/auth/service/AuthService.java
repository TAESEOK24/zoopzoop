package com.zoopzoop.zoopzoop.domain.auth.service;

import com.zoopzoop.zoopzoop.domain.auth.dto.request.LoginRequest;
import com.zoopzoop.zoopzoop.domain.auth.dto.request.PasswordResetRequest;
import com.zoopzoop.zoopzoop.domain.auth.dto.request.SignupRequest;
import com.zoopzoop.zoopzoop.domain.auth.dto.response.AuthResponse;
import com.zoopzoop.zoopzoop.domain.auth.dto.response.PasswordResetResponse;
import com.zoopzoop.zoopzoop.domain.user.dto.UserSummary;
import com.zoopzoop.zoopzoop.domain.user.entity.Role;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import com.zoopzoop.zoopzoop.global.security.jwt.JwtProvider;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import java.security.SecureRandom;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            JwtProvider jwtProvider,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(409, "Email is already registered.");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        String accessToken = jwtProvider.generateToken(savedUser);

        return AuthResponse.of(accessToken, UserSummary.from(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(401, "Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AppException(401, "Invalid email or password.");
        }

        String accessToken = jwtProvider.generateToken(user);

        return AuthResponse.of(accessToken, UserSummary.from(user));
    }

    @Transactional
    public PasswordResetResponse resetPassword(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(404, "User not found."));

        if (!user.getName().equals(request.name())) {
            throw new AppException(400, "Name does not match the registered account.");
        }

        String temporaryPassword = generateTemporaryPassword();
        user.changePassword(passwordEncoder.encode(temporaryPassword));

        return new PasswordResetResponse(temporaryPassword);
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length());
            password.append(TEMP_PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("auth", "auth module ready");
    }
}
