package com.zoopzoop.zoopzoop.domain.chatbot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zoopzoop.zoopzoop.domain.chatbot.client.ChatbotAiClient;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAskResponse;
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
    void askReturnsAiAnswerWhenPoliciesExist() {
        List<PolicySearchResultDto> policies = List.of(
                new PolicySearchResultDto(
                        "svc-1",
                        "청년 월세 지원",
                        "주거비 부담 완화",
                        "청년",
                        "월세 일부 지원",
                        "온라인 신청",
                        "상시",
                        "https://example.com/policies/svc-1",
                        "서울시",
                        "청년정책과"
                )
        );

        when(policySearchService.searchPolicies("청년 주거 지원 알려줘", 5)).thenReturn(policies);
        when(chatbotAiClient.generateAnswer("청년 주거 지원 알려줘", policies))
                .thenReturn("청년 월세 지원 정책이 있습니다.");

        ChatbotAskResponse response = chatbotService.ask("청년 주거 지원 알려줘");

        assertEquals("청년 월세 지원 정책이 있습니다.", response.answer());
        assertEquals(1, response.matchedPolicyCount());
        assertEquals("svc-1", response.references().get(0).serviceId());
    }

    @Test
    void askReturnsFallbackWithoutCallingAiWhenNoPoliciesFound() {
        when(policySearchService.searchPolicies("외계인 지원 정책 있어?", 5)).thenReturn(List.of());

        ChatbotAskResponse response = chatbotService.ask("외계인 지원 정책 있어?");

        assertEquals(0, response.matchedPolicyCount());
        verify(chatbotAiClient, never()).generateAnswer("외계인 지원 정책 있어?", List.of());
    }
}
