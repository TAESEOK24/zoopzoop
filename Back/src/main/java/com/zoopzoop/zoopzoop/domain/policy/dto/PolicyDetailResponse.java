package com.zoopzoop.zoopzoop.domain.policy.dto;

public record PolicyDetailResponse(
        String serviceId,
        String serviceName,
        String serviceType,
        String purposeSummary,
        String purpose,
        String target,
        String selectionCriteria,
        String supportContent,
        String applicationMethod,
        String applicationDeadline,
        String detailUrl,
        String orgName,
        String departmentName,
        String contactNumber,
        String contactInfo,
        String receivingOrg,
        String receivingOrgName,
        String requiredDocuments,
        String officialRequiredDocs,
        String userRequiredDocs,
        String onlineUrl,
        String adminRule,
        String localRule,
        String law,
        Integer viewCount,
        PolicyConditionTagsResponse conditions
) {
}
