package com.zoopzoop.zoopzoop.domain.chatbot.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatbotAskRequest(
        @NotBlank(message = "질문을 입력해주세요.")
        String message
) {
}
