package com.zoopzoop.zoopzoop.domain.user.controller;

import com.zoopzoop.zoopzoop.domain.user.dto.UserSummary;
import com.zoopzoop.zoopzoop.domain.user.dto.UserUpdateRequest;
import com.zoopzoop.zoopzoop.domain.user.service.UserService;
import com.zoopzoop.zoopzoop.global.security.AuthenticatedUser;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserSummary> me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(userService.getCurrentUser(currentUser.id()));
    }

    // 🚀 [수정됨] request.getName() 대신 request 객체를 통째로 넘겨줍니다!
    @PutMapping("/profile")
    public ApiResponse<String> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody UserUpdateRequest.Profile request) {

        userService.updateProfile(currentUser.id(), request); // 👈 바로 이 부분입니다!

        return ApiResponse.ok("프로필이 성공적으로 수정되었습니다.");
    }

    @PutMapping("/password")
    public ApiResponse<String> changePassword(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody UserUpdateRequest.Password request) {
        userService.changePassword(currentUser.id(), request.getCurrentPassword(), request.getNewPassword());
        return ApiResponse.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        userService.withdraw(currentUser.id());
        return ApiResponse.ok(null);
    }

    @GetMapping("/health")
    public ApiResponse<HealthCheckDto> health() {
        return ApiResponse.ok(userService.getStatus());
    }
}