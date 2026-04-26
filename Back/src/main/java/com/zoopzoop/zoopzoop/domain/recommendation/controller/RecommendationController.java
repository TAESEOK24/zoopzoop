package com.zoopzoop.zoopzoop.domain.recommendation.controller;

import com.zoopzoop.zoopzoop.domain.recommendation.dto.RecommendationResponse;
import com.zoopzoop.zoopzoop.domain.recommendation.service.RecommendationService;
import com.zoopzoop.zoopzoop.global.security.AuthenticatedUser;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/health")
    public ApiResponse<HealthCheckDto> health() {
        return ApiResponse.ok(recommendationService.getStatus());
    }

    @GetMapping("/personalized")
    public ApiResponse<RecommendationResponse> getPersonalizedRecommendations(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(recommendationService.getPersonalizedRecommendations(user, size));
    }
}
