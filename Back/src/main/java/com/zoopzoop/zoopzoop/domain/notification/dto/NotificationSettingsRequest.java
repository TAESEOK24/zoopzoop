package com.zoopzoop.zoopzoop.domain.notification.dto;

public record NotificationSettingsRequest(
        boolean deadlineSoon,
        boolean newPolicy,
        boolean recommendedPolicy
) {
}
