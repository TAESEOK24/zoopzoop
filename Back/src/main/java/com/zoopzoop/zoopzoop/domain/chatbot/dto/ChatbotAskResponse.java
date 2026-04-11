package com.zoopzoop.zoopzoop.domain.chatbot.dto;

import java.util.List;

public record ChatbotAskResponse(
        String answer,
        List<ChatbotReferenceDto> references,
        int matchedPolicyCount
) {
}
