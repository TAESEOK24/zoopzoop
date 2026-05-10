package com.zoopzoop.zoopzoop.domain.notification.controller;

import com.zoopzoop.zoopzoop.domain.notification.dto.NotificationGenerateResponse;
import com.zoopzoop.zoopzoop.domain.notification.dto.NotificationListResponse;
import com.zoopzoop.zoopzoop.domain.notification.dto.NotificationResponse;
import com.zoopzoop.zoopzoop.domain.notification.dto.NotificationSettingsRequest;
import com.zoopzoop.zoopzoop.domain.notification.dto.NotificationSettingsResponse;
import com.zoopzoop.zoopzoop.domain.notification.dto.UnreadCountResponse;
import com.zoopzoop.zoopzoop.domain.notification.service.NotificationService;
import com.zoopzoop.zoopzoop.global.security.AuthenticatedUser;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/health")
    public ApiResponse<HealthCheckDto> health() {
        return ApiResponse.ok(notificationService.getStatus());
    }

    @GetMapping("/recent")
    public ApiResponse<NotificationListResponse> recent(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ApiResponse.ok(notificationService.getRecentNotifications(user, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(notificationService.getUnreadCount(user));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long notificationId
    ) {
        return ApiResponse.ok(notificationService.markRead(user, notificationId));
    }

    @PatchMapping("/read-all")
    public ApiResponse<UnreadCountResponse> markAllRead(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(notificationService.markAllRead(user));
    }

    @GetMapping("/settings")
    public ApiResponse<NotificationSettingsResponse> settings(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(notificationService.getSettings(user));
    }

    @PutMapping("/settings")
    public ApiResponse<NotificationSettingsResponse> updateSettings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody NotificationSettingsRequest request
    ) {
        return ApiResponse.ok(notificationService.updateSettings(user, request));
    }

    @PostMapping("/deadline-soon/generate")
    public ApiResponse<NotificationGenerateResponse> generateDeadlineSoonNotifications() {
        int createdCount = notificationService.createDeadlineSoonNotifications();
        return ApiResponse.ok(new NotificationGenerateResponse(createdCount));
    }

    @PostMapping("/new-policy/generate")
    public ApiResponse<NotificationGenerateResponse> generateNewPolicyNotifications() {
        int createdCount = notificationService.createNewPolicyNotifications();
        return ApiResponse.ok(new NotificationGenerateResponse(createdCount));
    }

    @PostMapping("/recommended-policy/generate")
    public ApiResponse<NotificationGenerateResponse> generateRecommendedPolicyNotifications() {
        int createdCount = notificationService.createRecommendedPolicyNotifications();
        return ApiResponse.ok(new NotificationGenerateResponse(createdCount));
    }
}
