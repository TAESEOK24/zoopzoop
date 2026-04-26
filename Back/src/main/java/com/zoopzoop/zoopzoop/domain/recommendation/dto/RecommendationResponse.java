package com.zoopzoop.zoopzoop.domain.recommendation.dto;

import java.util.List;

public record RecommendationResponse(
        List<RecommendationItemResponse> items
) {
}
