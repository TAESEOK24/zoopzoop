package com.zoopzoop.zoopzoop.domain.policy.dto;

public record PolicySearchResultDto(
        String serviceId,
        String serviceName,
        String purposeSummary,
        String target,
        String supportContent,
        String applicationMethod,
        String applicationDeadline,
        String detailUrl,
        String orgName,
        String departmentName
) {
}
