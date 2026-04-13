package com.zoopzoop.zoopzoop.domain.chatbot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zoopzoop.zoopzoop.domain.chatbot.client.ChatbotAiClient;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAiResult;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAskResponse;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotConversationMessage;
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

    @Mock
    private ChatbotConversationMemory conversationMemory;

    @InjectMocks
    private ChatbotService chatbotService;

    @Test
    void askReturnsSessionAwareResponse() {
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

        when(conversationMemory.resolveSessionId(null)).thenReturn("session-1");
        when(conversationMemory.getRecentMessages("session-1"))
                .thenReturn(List.of(new ChatbotConversationMessage("assistant", "안녕하세요")));
        when(policySearchService.searchPolicies("housing help", 3)).thenReturn(policies);
        when(chatbotAiClient.generateAnswer(
                "housing help",
                List.of(new ChatbotConversationMessage("assistant", "안녕하세요")),
                policies
        )).thenReturn(new ChatbotAiResult(
                "청년 주거 지원 정책을 우선 확인해 보세요.",
                List.of(new ChatbotRecommendationDto("svc-1", "주거비 부담 완화와 직접 관련된 정책입니다."))
        ));

        ChatbotAskResponse response = chatbotService.ask(null, "housing help");

        assertEquals("session-1", response.sessionId());
        assertEquals("청년 주거 지원 정책을 우선 확인해 보세요.", response.answer());
        assertEquals(1, response.matchedPolicyCount());
        assertEquals(1, response.policies().size());
        verify(conversationMemory).appendUserMessage("session-1", "housing help");
        verify(conversationMemory).appendAssistantMessage("session-1", "청년 주거 지원 정책을 우선 확인해 보세요.");
    }

    @Test
    void askStillCallsAiWhenNoPoliciesFound() {
        when(conversationMemory.resolveSessionId("session-2")).thenReturn("session-2");
        when(conversationMemory.getRecentMessages("session-2")).thenReturn(List.of());
        when(policySearchService.searchPolicies("안녕", 3)).thenReturn(List.of());
        when(chatbotAiClient.generateAnswer("안녕", List.of(), List.of()))
                .thenReturn(new ChatbotAiResult("안녕하세요. 어떤 정책이 궁금하신가요?", List.of()));

        ChatbotAskResponse response = chatbotService.ask("session-2", "안녕");

        assertEquals("session-2", response.sessionId());
        assertEquals("안녕하세요. 어떤 정책이 궁금하신가요?", response.answer());
        assertEquals(0, response.matchedPolicyCount());
        assertEquals(0, response.policies().size());
    }
}
