package com.zoopzoop.zoopzoop.domain.admin.controller;

import com.zoopzoop.zoopzoop.domain.admin.dto.*;
import com.zoopzoop.zoopzoop.domain.admin.service.AdminService;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // 📊 대시보드 통계 API
    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboardStats() {
        return ApiResponse.ok(adminService.getDashboardStats());
    }

    // 👥 유저 목록 API
    @GetMapping("/users")
    public ApiResponse<List<AdminUserResponse>> getAllUsers() {
        return ApiResponse.ok(adminService.getAllUsers());
    }

    // 🚫 유저 강제 탈퇴 API
    @DeleteMapping("/users/{userId}")
    public ApiResponse<String> forceDeleteUser(@PathVariable Long userId) {
        adminService.forceDeleteUser(userId);
        return ApiResponse.ok("해당 유저가 성공적으로 삭제되었습니다.");
    }

    // 📝 게시글 목록 API
    @GetMapping("/posts")
    public ApiResponse<List<AdminPostResponse>> getAllPosts() {
        return ApiResponse.ok(adminService.getAllPosts());
    }

    // 🗑️ 게시글 삭제 API
    @DeleteMapping("/posts/{postId}")
    public ApiResponse<String> forceDeletePost(@PathVariable Long postId) {
        adminService.forceDeletePost(postId);
        return ApiResponse.ok("게시글이 성공적으로 삭제되었습니다.");
    }

    // ⚙️ 정책 강제 동기화 API
    @PostMapping("/policies/sync")
    public ApiResponse<String> syncPolicies() {
        String result = adminService.syncPolicies();
        return ApiResponse.ok(result); // 성공 메시지 혹은 실패 로그를 프론트로 전달
    }
}