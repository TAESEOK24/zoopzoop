package com.zoopzoop.zoopzoop.domain.policy.dto;

import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;

public record PolicySummaryResponse(
        String serviceId,
        String serviceName,
        String serviceType,
        String purposeSummary,
        String applicationDeadline,
        String orgName,
        String departmentName,
        String detailUrl,
        Integer viewCount
) {
    public static PolicySummaryResponse from(PolicyList policy) {
        return new PolicySummaryResponse(
                policy.getServiceId(),
                policy.getServiceName(),
                policy.getServiceType(),
                policy.getPurposeSummary(),
                policy.getApplicationDeadline(),
                policy.getOrgName(),
                policy.getDepartmentName(),
                policy.getDetailUrl(),
                policy.getViewCount()
        );
    }
}
