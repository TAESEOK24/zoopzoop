package com.zoopzoop.zoopzoop.domain.user.service;

import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("user", "user module ready");
    }
}
