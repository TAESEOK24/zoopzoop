package com.zoopzoop.zoopzoop.domain.auth.service;

import com.zoopzoop.zoopzoop.domain.auth.dto.response.AuthResponse;
import com.zoopzoop.zoopzoop.domain.user.dto.UserSummary;
import com.zoopzoop.zoopzoop.domain.user.entity.Role;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import com.zoopzoop.zoopzoop.global.security.jwt.JwtProvider;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String authUri;
    private final String tokenUri;
    private final String userInfoUri;

    public GoogleOAuthService(
            UserRepository userRepository,
            JwtProvider jwtProvider,
            PasswordEncoder passwordEncoder,
            RestClient.Builder restClientBuilder,
            @Value("${oauth.google.client-id:}") String clientId,
            @Value("${oauth.google.client-secret:}") String clientSecret,
            @Value("${oauth.google.backend-redirect-uri}") String redirectUri,
            @Value("${oauth.google.auth-uri}") String authUri,
            @Value("${oauth.google.token-uri}") String tokenUri,
            @Value("${oauth.google.user-info-uri}") String userInfoUri
    ) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authUri = authUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
    }

    public String buildAuthorizationUrl() {
        validateGoogleConfig();

        return UriComponentsBuilder.fromUriString(authUri)
                .queryParam("scope", "openid email profile")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "select_account")
                .build()
                .encode()
                .toUriString();
    }

    @Transactional
    public AuthResponse loginWithCode(String code) {
        validateGoogleConfig();

        GoogleTokenResponse tokenResponse = requestAccessToken(code);
        GoogleUserInfoResponse userInfo = requestUserInfo(tokenResponse.accessToken());

        if (!Boolean.TRUE.equals(userInfo.emailVerified())) {
            throw new AppException(401, "Google email is not verified.");
        }

        User user = userRepository.findByEmail(userInfo.email())
                .orElseGet(() -> createGoogleUser(userInfo));
        String accessToken = jwtProvider.generateToken(user);

        return AuthResponse.of(accessToken, UserSummary.from(user));
    }

    private GoogleTokenResponse requestAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        try {
            GoogleTokenResponse response = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(GoogleTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new AppException(502, "Google access token response is invalid.");
            }

            return response;
        } catch (RestClientException exception) {
            throw new AppException(502, "Failed to request Google access token.");
        }
    }

    private GoogleUserInfoResponse requestUserInfo(String accessToken) {
        try {
            GoogleUserInfoResponse response = restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);

            if (response == null || response.email() == null || response.email().isBlank()) {
                throw new AppException(502, "Google user info response is invalid.");
            }

            return response;
        } catch (RestClientException exception) {
            throw new AppException(502, "Failed to request Google user info.");
        }
    }

    private User createGoogleUser(GoogleUserInfoResponse userInfo) {
        User user = User.builder()
                .email(userInfo.email())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .name(resolveName(userInfo))
                .role(Role.USER)
                .build();

        return userRepository.save(user);
    }

    private String resolveName(GoogleUserInfoResponse userInfo) {
        if (userInfo.name() != null && !userInfo.name().isBlank()) {
            return userInfo.name();
        }

        return userInfo.email().substring(0, userInfo.email().indexOf('@'));
    }

    private void validateGoogleConfig() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new AppException(500, "Google OAuth client configuration is missing.");
        }
    }

    private record GoogleTokenResponse(
            @com.fasterxml.jackson.annotation.JsonProperty("access_token")
            String accessToken
    ) {
    }

    private record GoogleUserInfoResponse(
            String email,
            @com.fasterxml.jackson.annotation.JsonProperty("email_verified")
            Boolean emailVerified,
            String name
    ) {
    }
}
