package com.zoopzoop.zoopzoop.domain.user.controller;

import com.zoopzoop.zoopzoop.domain.user.dto.UserSummary;
import com.zoopzoop.zoopzoop.domain.user.service.UserService;
import com.zoopzoop.zoopzoop.global.security.AuthenticatedUser;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserSummary> me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponse.ok(userService.getCurrentUser(authenticatedUser.id()));
    }

    @GetMapping("/health")
    public ApiResponse<HealthCheckDto> health() {
        return ApiResponse.ok(userService.getStatus());
    }
}
