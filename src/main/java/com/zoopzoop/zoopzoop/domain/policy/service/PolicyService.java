package com.zoopzoop.zoopzoop.domain.policy.service;

import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import org.springframework.stereotype.Service;

@Service
public class PolicyService {

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("policy", "policy module ready");
    }
}
