package com.zoopzoop.zoopzoop.domain.recommendation.service;

import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("recommendation", "recommendation module ready");
    }
}
