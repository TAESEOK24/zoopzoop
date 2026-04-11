package com.zoopzoop.zoopzoop.domain.chatbot.dto;

public record ChatbotPolicyDto(
        String serviceId,
        String serviceName,
        String purposeSummary,
        String target,
        String supportContent,
        String applicationMethod,
        String applicationDeadline,
        String detailUrl,
        String orgName,
        String departmentName,
        String recommendationReason
) {
}
