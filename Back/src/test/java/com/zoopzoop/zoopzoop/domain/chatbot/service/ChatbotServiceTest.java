package com.zoopzoop.zoopzoop.domain.chatbot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zoopzoop.zoopzoop.domain.chatbot.client.ChatbotAiClient;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAiResult;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAskResponse;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotConversationMessage;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotRecommendationDto;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotResponseType;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySearchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(
                policySearchService,
                chatbotAiClient,
                conversationMemory,
                new ChatbotIntentClassifier(),
                new ChatbotIntakeMemory()
        );
    }

    @Test
    void askReturnsPolicyResponseForDirectPolicyQuestion() {
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
        when(policySearchService.searchPolicies("청년 주거 지원 알려줘", 3, null)).thenReturn(policies);
        when(chatbotAiClient.generateAnswer(
                "청년 주거 지원 알려줘",
                List.of(new ChatbotConversationMessage("assistant", "안녕하세요")),
                policies
        )).thenReturn(new ChatbotAiResult(
                "청년 주거 지원 정책을 찾았어요.",
                List.of(new ChatbotRecommendationDto("svc-1", "주거비 부담을 줄이는 데 도움이 됩니다."))
        ));

        ChatbotAskResponse response = chatbotService.ask(null, "청년 주거 지원 알려줘");

        assertEquals("session-1", response.sessionId());
        assertEquals(ChatbotResponseType.POLICY_SEARCH, response.responseType());
        assertEquals("청년 주거 지원 정책을 찾았어요.", response.answer());
        assertEquals(1, response.matchedPolicyCount());
        assertEquals(1, response.policies().size());
        assertEquals(3, response.suggestedReplies().size());
        verify(conversationMemory).appendUserMessage("session-1", "청년 주거 지원 알려줘");
        verify(conversationMemory).appendAssistantMessage("session-1", "청년 주거 지원 정책을 찾았어요.");
    }

    @Test
    void askClarifiesBroadYouthPolicyQuestionWithoutPolicySearch() {
        when(conversationMemory.resolveSessionId("session-youth")).thenReturn("session-youth");
        when(conversationMemory.getRecentMessages("session-youth")).thenReturn(List.of());

        ChatbotAskResponse response = chatbotService.ask("session-youth", "청년 정책 관련해서 궁금하게 있어");

        assertEquals(ChatbotResponseType.CLARIFICATION_NEEDED, response.responseType());
        assertTrue(response.answer().contains("어떤 분야가 궁금하세요"));
        assertEquals(0, response.matchedPolicyCount());
        assertEquals(4, response.suggestedReplies().size());
        verify(policySearchService, never()).searchPolicies(anyString(), eq(3));
        verify(chatbotAiClient, never()).generateAnswer(anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void askClarifiesBroadYouthPolicyQuestionBeforeAiClassification() {
        when(conversationMemory.resolveSessionId("session-youth-ai")).thenReturn("session-youth-ai");
        when(conversationMemory.getRecentMessages("session-youth-ai")).thenReturn(List.of());

        ChatbotAskResponse response = chatbotService.ask("session-youth-ai", "청년 정책에 대해서 알려줘");

        assertEquals(ChatbotResponseType.CLARIFICATION_NEEDED, response.responseType());
        assertTrue(response.answer().contains("어떤 분야가 궁금하세요"));
        assertEquals(4, response.suggestedReplies().size());
        verify(policySearchService, never()).searchPolicies(anyString(), eq(3));
        verify(chatbotAiClient, never()).classifyIntent(anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.any());
        verify(chatbotAiClient, never()).generateAnswer(anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void askSearchesSpecificJobSupportQuestion() {
        List<PolicySearchResultDto> policies = List.of(
                new PolicySearchResultDto(
                        "svc-job",
                        "Youth Job Support",
                        "Supports job seekers.",
                        "Job seekers",
                        "Employment support",
                        "Online application",
                        "Always open",
                        "https://example.com/policies/svc-job",
                        "Seoul",
                        "Job Team"
                )
        );

        when(conversationMemory.resolveSessionId("session-job")).thenReturn("session-job");
        when(conversationMemory.getRecentMessages("session-job")).thenReturn(List.of());
        when(policySearchService.searchPolicies("구직 중인데 받을 수 있는 지원 정책 있어?", 3, null)).thenReturn(policies);
        when(chatbotAiClient.generateAnswer("구직 중인데 받을 수 있는 지원 정책 있어?", List.of(), policies))
                .thenReturn(new ChatbotAiResult("구직 지원 정책을 찾았어요.", List.of()));

        ChatbotAskResponse response = chatbotService.ask("session-job", "구직 중인데 받을 수 있는 지원 정책 있어?");

        assertEquals(ChatbotResponseType.POLICY_SEARCH, response.responseType());
        assertEquals(1, response.matchedPolicyCount());
    }

    @Test
    void askStartsClarificationFlowForAmbiguousHardshipMessage() {
        when(conversationMemory.resolveSessionId("session-2")).thenReturn("session-2");
        when(conversationMemory.getRecentMessages("session-2")).thenReturn(List.of());

        ChatbotAskResponse response = chatbotService.ask("session-2", "나는 너무 가난한 것 같아");

        assertEquals(ChatbotResponseType.CLARIFICATION_NEEDED, response.responseType());
        assertTrue(response.answer().contains("연령대"));
        assertEquals(0, response.matchedPolicyCount());
        assertEquals(2, response.suggestedReplies().size());
        verify(policySearchService, never()).searchPolicies(anyString(), eq(3));
        verify(chatbotAiClient, never()).generateAnswer(anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void askContinuesClarificationAndThenSearchesAfterEnoughContext() {
        List<PolicySearchResultDto> policies = List.of(
                new PolicySearchResultDto(
                        "svc-1",
                        "Youth One-Person Housing Support",
                        "Supports one-person young households.",
                        "Young single-person households",
                        "Monthly rent support",
                        "Online application",
                        "Always open",
                        "https://example.com/policies/svc-1",
                        "Seoul",
                        "Youth Policy Team"
                )
        );

        when(conversationMemory.resolveSessionId("session-3")).thenReturn("session-3");
        when(conversationMemory.getRecentMessages("session-3")).thenReturn(List.of());
        when(policySearchService.searchPolicies("나는 너무 가난한 것 같아 청년 1인 가구", 3, null)).thenReturn(policies);
        when(chatbotAiClient.generateAnswer("나는 너무 가난한 것 같아", List.of(), policies))
                .thenReturn(new ChatbotAiResult(
                        "청년 1인 가구에 맞는 지원 정책을 찾았어요.",
                        List.of(new ChatbotRecommendationDto("svc-1", "청년 1인 가구의 주거 부담과 연결됩니다."))
                ));

        ChatbotAskResponse firstResponse = chatbotService.ask("session-3", "나는 너무 가난한 것 같아");
        ChatbotAskResponse secondResponse = chatbotService.ask("session-3", "청년");
        ChatbotAskResponse thirdResponse = chatbotService.ask("session-3", "혼자 살아요");

        assertEquals(ChatbotResponseType.CLARIFICATION_NEEDED, firstResponse.responseType());
        assertEquals(ChatbotResponseType.CLARIFICATION_NEEDED, secondResponse.responseType());
        assertEquals(ChatbotResponseType.POLICY_SEARCH, thirdResponse.responseType());
        assertEquals(1, thirdResponse.matchedPolicyCount());
        verify(policySearchService).searchPolicies("나는 너무 가난한 것 같아 청년 1인 가구", 3, null);
    }

    @Test
    void askTreatsNumericAgeAsAgeGroupDuringClarification() {
        List<PolicySearchResultDto> policies = List.of(
                new PolicySearchResultDto(
                        "svc-1",
                        "Youth One-Person Housing Support",
                        "Supports one-person young households.",
                        "Young single-person households",
                        "Monthly rent support",
                        "Online application",
                        "Always open",
                        "https://example.com/policies/svc-1",
                        "Seoul",
                        "Youth Policy Team"
                )
        );

        when(conversationMemory.resolveSessionId("session-age")).thenReturn("session-age");
        when(conversationMemory.getRecentMessages("session-age")).thenReturn(List.of());
        when(policySearchService.searchPolicies("나는 너무 가난한 것 같아 청년 25세 1인 가구", 3, 25)).thenReturn(policies);
        when(chatbotAiClient.generateAnswer("나는 너무 가난한 것 같아", List.of(), policies))
                .thenReturn(new ChatbotAiResult(
                        "청년 1인 가구에 맞는 지원 정책을 찾았어요.",
                        List.of(new ChatbotRecommendationDto("svc-1", "청년 1인 가구의 주거 부담과 연결됩니다."))
                ));

        ChatbotAskResponse firstResponse = chatbotService.ask("session-age", "나는 너무 가난한 것 같아");
        ChatbotAskResponse secondResponse = chatbotService.ask("session-age", "25살이에요");
        ChatbotAskResponse thirdResponse = chatbotService.ask("session-age", "혼자 살아요");

        assertEquals(ChatbotResponseType.CLARIFICATION_NEEDED, firstResponse.responseType());
        assertEquals(ChatbotResponseType.CLARIFICATION_NEEDED, secondResponse.responseType());
        assertFalse(secondResponse.answer().contains("연령대"));
        assertTrue(secondResponse.answer().contains("혼자"));
        assertEquals(ChatbotResponseType.POLICY_SEARCH, thirdResponse.responseType());
        verify(policySearchService).searchPolicies("나는 너무 가난한 것 같아 청년 25세 1인 가구", 3, 25);
    }

    @Test
    void askReturnsSmalltalkResponseWithoutPolicySearch() {
        when(conversationMemory.resolveSessionId("session-4")).thenReturn("session-4");
        when(conversationMemory.getRecentMessages("session-4")).thenReturn(List.of());

        ChatbotAskResponse response = chatbotService.ask("session-4", "안녕");

        assertEquals(ChatbotResponseType.SMALLTALK, response.responseType());
        assertEquals(0, response.matchedPolicyCount());
        verify(policySearchService, never()).searchPolicies(anyString(), eq(3));
    }

    @Test
    void askReturnsSafetyResponseForRiskMessage() {
        when(conversationMemory.resolveSessionId("session-5")).thenReturn("session-5");
        when(conversationMemory.getRecentMessages("session-5")).thenReturn(List.of());

        ChatbotAskResponse response = chatbotService.ask("session-5", "죽고 싶어");

        assertEquals(ChatbotResponseType.SAFETY, response.responseType());
        assertTrue(response.answer().contains("109"));
        verify(policySearchService, never()).searchPolicies(anyString(), eq(3));
    }
}

