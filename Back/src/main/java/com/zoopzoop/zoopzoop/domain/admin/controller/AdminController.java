package com.zoopzoop.zoopzoop.domain.admin.controller;

import com.zoopzoop.zoopzoop.domain.admin.dto.*;
import com.zoopzoop.zoopzoop.domain.admin.service.AdminService;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
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

    @GetMapping("/health")
    public ApiResponse<HealthCheckDto> health() {
        return ApiResponse.ok(adminService.getStatus());
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboardStats() {
        return ApiResponse.ok(adminService.getDashboardStats());
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserResponse>> getAllUsers() {
        return ApiResponse.ok(adminService.getAllUsers());
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<String> forceDeleteUser(@PathVariable Long userId) {
        adminService.forceDeleteUser(userId);
        return ApiResponse.ok("해당 유저가 성공적으로 삭제되었습니다.");
    }

    @GetMapping("/posts")
    public ApiResponse<List<AdminPostResponse>> getAllPosts() {
        return ApiResponse.ok(adminService.getAllPosts());
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<String> forceDeletePost(@PathVariable Long postId) {
        adminService.forceDeletePost(postId);
        return ApiResponse.ok("게시글이 성공적으로 삭제되었습니다.");
    }

    @PostMapping("/policies/sync")
    public ApiResponse<String> syncPolicies() {
        String result = adminService.syncPolicies();
        return ApiResponse.ok(result);
    }

    // 🚨 신고 관리 API
    @GetMapping("/reports")
    public ApiResponse<List<AdminReportResponse>> getPendingReports() {
        return ApiResponse.ok(adminService.getPendingReports());
    }

    @PutMapping("/reports/{reportId}/resolve")
    public ApiResponse<String> resolveReport(@PathVariable Long reportId) {
        adminService.resolveReport(reportId);
        return ApiResponse.ok("신고 처리가 완료되었습니다.");
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<String> forceDeleteComment(@PathVariable Long commentId) {
        adminService.forceDeleteComment(commentId);
        return ApiResponse.ok("댓글이 강제로 삭제되었습니다.");
    }
}