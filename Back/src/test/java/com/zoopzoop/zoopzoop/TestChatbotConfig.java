package com.zoopzoop.zoopzoop;

import com.zoopzoop.zoopzoop.domain.chatbot.client.ChatbotAiClient;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAiResult;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotRecommendationDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestChatbotConfig {

    @Bean
    @Primary
    public ChatbotAiClient chatbotAiClient() {
        return new ChatbotAiClient() {
            @Override
            public ChatbotAiResult generateAnswer(String userMessage, List<PolicySearchResultDto> policies) {
                List<ChatbotRecommendationDto> recommendations = policies.stream()
                        .limit(3)
                        .map(policy -> new ChatbotRecommendationDto(
                                policy.serviceId(),
                                "테스트 환경 기본 추천 사유"
                        ))
                        .toList();

                return new ChatbotAiResult("테스트 환경 기본 응답", recommendations);
            }
        };
    }
}
