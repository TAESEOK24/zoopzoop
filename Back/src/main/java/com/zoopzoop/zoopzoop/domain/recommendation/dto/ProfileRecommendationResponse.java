package com.zoopzoop.zoopzoop.domain.recommendation.dto;

import java.util.List;

public record ProfileRecommendationResponse(
        boolean profileReady,
        List<RecommendationItemResponse> items
) {
    public static ProfileRecommendationResponse notReady() {
        return new ProfileRecommendationResponse(false, List.of());
    }

    public static ProfileRecommendationResponse ready(List<RecommendationItemResponse> items) {
        return new ProfileRecommendationResponse(true, items);
    }
}
