package com.zoopzoop.zoopzoop.domain.chatbot.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatbotAskRequest(
        String sessionId,
        @NotBlank(message = "질문을 입력해 주세요.")
        String message
) {
}
