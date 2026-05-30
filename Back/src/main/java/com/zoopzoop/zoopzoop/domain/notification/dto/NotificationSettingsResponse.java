package com.zoopzoop.zoopzoop.domain.notification.dto;

import com.zoopzoop.zoopzoop.domain.notification.entity.NotificationSetting;

public record NotificationSettingsResponse(
        boolean deadlineSoon,
        boolean newPolicy,
        boolean recommendedPolicy
) {
    public static NotificationSettingsResponse from(NotificationSetting setting) {
        return new NotificationSettingsResponse(
                setting.isDeadlineSoonEnabled(),
                setting.isNewPolicyEnabled(),
                setting.isRecommendedPolicyEnabled()
        );
    }
}
