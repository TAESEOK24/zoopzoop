package com.zoopzoop.zoopzoop.domain.admin.service;

import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("admin", "admin module ready");
    }
}
