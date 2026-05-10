package com.zoopzoop.zoopzoop.domain.notification.dto;

import com.zoopzoop.zoopzoop.domain.notification.entity.Notification;
import com.zoopzoop.zoopzoop.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String serviceId,
        String policyName,
        NotificationType type,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getPolicy().getServiceId(),
                notification.getPolicyName(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isReadStatus(),
                notification.getCreatedAt()
        );
    }
}
