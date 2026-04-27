package com.zoopzoop.zoopzoop.domain.chatbot.dto;

import java.util.List;

public record ChatbotAskResponse(
        String sessionId,
        String answer,
        ChatbotResponseType responseType,
        List<ChatbotSuggestedReplyDto> suggestedReplies,
        List<ChatbotPolicyDto> policies,
        List<ChatbotReferenceDto> references,
        int matchedPolicyCount
) {
}
