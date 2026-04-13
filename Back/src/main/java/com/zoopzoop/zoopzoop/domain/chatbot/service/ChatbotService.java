package com.zoopzoop.zoopzoop.domain.chatbot.service;

import com.zoopzoop.zoopzoop.domain.chatbot.client.ChatbotAiClient;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAiResult;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAskResponse;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotConversationMessage;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotPolicyDto;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotRecommendationDto;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotReferenceDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySearchService;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final int SEARCH_SIZE = 3;

    private final PolicySearchService policySearchService;
    private final ChatbotAiClient chatbotAiClient;
    private final ChatbotConversationMemory conversationMemory;

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("chatbot", "chatbot module ready");
    }

    public ChatbotAskResponse ask(String sessionId, String message) {
        String resolvedSessionId = conversationMemory.resolveSessionId(sessionId);
        List<ChatbotConversationMessage> history = conversationMemory.getRecentMessages(resolvedSessionId);
        List<PolicySearchResultDto> policies = policySearchService.searchPolicies(message, SEARCH_SIZE);
        List<ChatbotReferenceDto> references = policies.stream()
                .map(policy -> new ChatbotReferenceDto(
                        policy.serviceId(),
                        policy.serviceName(),
                        policy.detailUrl()
                ))
                .toList();

        ChatbotAiResult aiResult = chatbotAiClient.generateAnswer(message, history, policies);
        Map<String, String> reasonsByServiceId = aiResult.recommendations().stream()
                .collect(Collectors.toMap(
                        ChatbotRecommendationDto::serviceId,
                        ChatbotRecommendationDto::reason,
                        (left, right) -> left
                ));

        List<ChatbotPolicyDto> policyCards = policies.stream()
                .map(policy -> new ChatbotPolicyDto(
                        policy.serviceId(),
                        policy.serviceName(),
                        policy.purposeSummary(),
                        policy.target(),
                        policy.supportContent(),
                        policy.applicationMethod(),
                        policy.applicationDeadline(),
                        policy.detailUrl(),
                        policy.orgName(),
                        policy.departmentName(),
                        reasonsByServiceId.getOrDefault(policy.serviceId(), "질문과 연관된 정책입니다.")
                ))
                .toList();

        conversationMemory.appendUserMessage(resolvedSessionId, message);
        conversationMemory.appendAssistantMessage(resolvedSessionId, aiResult.summary());

        return new ChatbotAskResponse(
                resolvedSessionId,
                aiResult.summary(),
                policyCards,
                references,
                policies.size()
        );
    }
}
