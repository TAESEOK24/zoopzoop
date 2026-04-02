package com.zoopzoop.zoopzoop.domain.community.service;

import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import org.springframework.stereotype.Service;

@Service
public class CommunityService {

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("community", "community module ready");
    }
}
