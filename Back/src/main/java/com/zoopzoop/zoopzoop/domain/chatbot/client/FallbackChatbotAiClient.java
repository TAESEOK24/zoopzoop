package com.zoopzoop.zoopzoop.domain.chatbot.client;

import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAiResult;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotRecommendationDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(ChatClient.Builder.class)
public class FallbackChatbotAiClient implements ChatbotAiClient {

    private static final String FALLBACK_SUMMARY =
            "AI 응답을 사용할 수 없어 검색 결과 기준으로 관련 정책을 정리했습니다.";

    @Override
    public ChatbotAiResult generateAnswer(String userMessage, List<PolicySearchResultDto> policies) {
        List<ChatbotRecommendationDto> recommendations = policies.stream()
                .limit(3)
                .map(policy -> new ChatbotRecommendationDto(
                        policy.serviceId(),
                        "질문과 연관도가 높아 우선 확인할 만한 정책입니다."
                ))
                .toList();

        return new ChatbotAiResult(FALLBACK_SUMMARY, recommendations);
    }
}
