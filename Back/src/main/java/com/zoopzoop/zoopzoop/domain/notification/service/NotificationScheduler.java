package com.zoopzoop.zoopzoop.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationScheduler {

    private final NotificationService notificationService;

    public NotificationScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${notification.deadline.cron:0 0 9 * * *}", zone = "${notification.zone:Asia/Seoul}")
    public void createDeadlineSoonNotifications() {
        int createdCount = notificationService.createDeadlineSoonNotifications();
        log.info("Deadline notification job completed. createdCount={}", createdCount);
    }

    @Scheduled(cron = "${notification.new-policy.cron:0 10 9 * * *}", zone = "${notification.zone:Asia/Seoul}")
    public void createNewPolicyNotifications() {
        int createdCount = notificationService.createNewPolicyNotifications();
        log.info("New policy notification job completed. createdCount={}", createdCount);
    }

    @Scheduled(cron = "${notification.recommended-policy.cron:0 20 9 * * *}", zone = "${notification.zone:Asia/Seoul}")
    public void createRecommendedPolicyNotifications() {
        int createdCount = notificationService.createRecommendedPolicyNotifications();
        log.info("Recommended policy notification job completed. createdCount={}", createdCount);
    }
}
