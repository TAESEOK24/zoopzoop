package com.zoopzoop.zoopzoop.domain.user.controller;

import com.zoopzoop.zoopzoop.domain.user.dto.UserSummary;
import com.zoopzoop.zoopzoop.domain.user.service.UserService;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // 🚀 [수정됨] 에러 없는 안전한 내 정보 조회 로직
    @GetMapping("/me")
    public ApiResponse<UserSummary> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentInfo = authentication.getName();

        String parsedEmail = currentInfo;
        if (currentInfo.contains("email=")) {
            int start = currentInfo.indexOf("email=") + 6;
            int end = currentInfo.indexOf(",", start);
            if (end == -1) end = currentInfo.indexOf("]", start);
            if (end != -1) parsedEmail = currentInfo.substring(start, end).trim();
        }

        // 추출한 이메일로 정보 조회해서 프론트로 반환
        return ApiResponse.ok(userService.getCurrentUserByEmail(parsedEmail));
    }

    @GetMapping("/health")
    public ApiResponse<HealthCheckDto> health() {
        return ApiResponse.ok(userService.getStatus());
    }
}