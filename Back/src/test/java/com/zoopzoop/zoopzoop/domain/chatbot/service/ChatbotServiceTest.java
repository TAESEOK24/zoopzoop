package com.zoopzoop.zoopzoop.domain.chatbot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zoopzoop.zoopzoop.domain.chatbot.client.ChatbotAiClient;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAiResult;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAskResponse;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotRecommendationDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySearchService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private PolicySearchService policySearchService;

    @Mock
    private ChatbotAiClient chatbotAiClient;

    @InjectMocks
    private ChatbotService chatbotService;

    @Test
    void askReturnsAiSummaryAndPolicyCardsWhenPoliciesExist() {
        List<PolicySearchResultDto> policies = List.of(
                new PolicySearchResultDto(
                        "svc-1",
                        "Youth Housing Support",
                        "Supports housing costs for young adults.",
                        "Young adults",
                        "Rent and deposit support",
                        "Online application",
                        "Always open",
                        "https://example.com/policies/svc-1",
                        "Seoul",
                        "Youth Policy Team"
                )
        );

        when(policySearchService.searchPolicies("housing help", 3)).thenReturn(policies);
        when(chatbotAiClient.generateAnswer("housing help", policies))
                .thenReturn(new ChatbotAiResult(
                        "청년 주거 지원 정책을 우선 확인해 보세요.",
                        List.of(new ChatbotRecommendationDto("svc-1", "주거비 부담 완화와 직접 관련된 정책입니다."))
                ));

        ChatbotAskResponse response = chatbotService.ask("housing help");

        assertEquals("청년 주거 지원 정책을 우선 확인해 보세요.", response.answer());
        assertEquals(1, response.matchedPolicyCount());
        assertEquals(1, response.references().size());
        assertEquals("svc-1", response.references().get(0).serviceId());
        assertEquals(1, response.policies().size());
        assertEquals("svc-1", response.policies().get(0).serviceId());
        assertEquals("주거비 부담 완화와 직접 관련된 정책입니다.", response.policies().get(0).recommendationReason());
    }

    @Test
    void askReturnsFallbackWithoutCallingAiWhenNoPoliciesFound() {
        when(policySearchService.searchPolicies("unknown question", 3)).thenReturn(List.of());

        ChatbotAskResponse response = chatbotService.ask("unknown question");

        assertEquals(0, response.matchedPolicyCount());
        assertEquals(0, response.policies().size());
        assertEquals(0, response.references().size());
        verify(chatbotAiClient, never()).generateAnswer("unknown question", List.of());
    }
}
