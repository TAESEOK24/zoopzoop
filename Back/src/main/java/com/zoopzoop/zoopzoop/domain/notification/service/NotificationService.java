package com.zoopzoop.zoopzoop.domain.notification.service;

import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("notification", "notification module ready");
    }
}
