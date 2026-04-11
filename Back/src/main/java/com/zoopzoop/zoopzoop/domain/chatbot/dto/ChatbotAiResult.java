package com.zoopzoop.zoopzoop.domain.chatbot.dto;

import java.util.List;

public record ChatbotAiResult(
        String summary,
        List<ChatbotRecommendationDto> recommendations
) {
}
