package com.zoopzoop.zoopzoop.domain.recommendation.dto;

import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;

public record RecommendationItemResponse(
        String serviceId,
        String serviceName,
        String serviceType,
        String purposeSummary,
        String applicationDeadline,
        String orgName,
        String departmentName,
        String detailUrl,
        Integer viewCount,
        String reason,
        double score
) {
    public static RecommendationItemResponse of(PolicyList policy, String reason, double score) {
        return new RecommendationItemResponse(
                policy.getServiceId(),
                policy.getServiceName(),
                policy.getServiceType(),
                policy.getPurposeSummary(),
                policy.getApplicationDeadline(),
                policy.getOrgName(),
                policy.getDepartmentName(),
                policy.getDetailUrl(),
                policy.getViewCount(),
                reason,
                score
        );
    }
}
