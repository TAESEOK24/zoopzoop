package com.zoopzoop.zoopzoop.domain.chatbot.dto;

import java.util.List;

public record ChatbotAskResponse(
        String answer,
        List<ChatbotPolicyDto> policies,
        List<ChatbotReferenceDto> references,
        int matchedPolicyCount
) {
}
