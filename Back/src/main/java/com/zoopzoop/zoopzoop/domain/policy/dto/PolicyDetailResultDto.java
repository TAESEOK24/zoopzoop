package com.zoopzoop.zoopzoop.domain.policy.dto;

public record PolicyDetailResultDto(
        String serviceId,
        String serviceName,
        String purposeSummary,
        String target,
        String selectionCriteria,
        String supportContent,
        String applicationMethod,
        String applicationDeadline,
        String detailUrl,
        String orgName,
        String departmentName,
        String contactNumber,
        String purpose,
        String requiredDocuments,
        String receivingOrgName,
        String contactInfo,
        String onlineUrl,
        String adminRule,
        String localRule,
        String law,
        String officialRequiredDocs,
        String userRequiredDocs
) {
}
