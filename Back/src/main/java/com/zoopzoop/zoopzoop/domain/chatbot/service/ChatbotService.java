package com.zoopzoop.zoopzoop.domain.chatbot.service;

import com.zoopzoop.zoopzoop.domain.chatbot.client.ChatbotAiClient;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAskResponse;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotReferenceDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySearchService;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final int SEARCH_SIZE = 5;
    private static final String NO_POLICY_MESSAGE = "질문과 관련된 정책을 찾지 못했습니다. 대상, 지역, 상황을 조금 더 구체적으로 입력해주세요.";

    private final PolicySearchService policySearchService;
    private final ChatbotAiClient chatbotAiClient;

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("chatbot", "chatbot module ready");
    }

    public ChatbotAskResponse ask(String message) {
        List<PolicySearchResultDto> policies = policySearchService.searchPolicies(message, SEARCH_SIZE);
        List<ChatbotReferenceDto> references = policies.stream()
                .map(policy -> new ChatbotReferenceDto(
                        policy.serviceId(),
                        policy.serviceName(),
                        policy.detailUrl()
                ))
                .toList();

        if (policies.isEmpty()) {
            return new ChatbotAskResponse(NO_POLICY_MESSAGE, references, 0);
        }

        String answer = chatbotAiClient.generateAnswer(message, policies);
        return new ChatbotAskResponse(answer, references, policies.size());
    }
}
