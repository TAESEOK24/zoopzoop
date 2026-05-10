package com.zoopzoop.zoopzoop.domain.notification.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> items
) {
}
