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
    void askSearchesStartupPolicyKeywordWithoutProfileContext() {
        List<PolicySearchResultDto> policies = List.of(
                new PolicySearchResultDto(
                        "svc-startup",
                        "Startup Grant",
                        "Supports startup costs.",
                        "Founders",
                        "Grant support",
                        "Online application",
                        "Always open",
                        "https://example.com/policies/svc-startup",
                        "Seoul",
                        "Startup Team"
                )
        );

        when(conversationMemory.resolveSessionId("session-startup")).thenReturn("session-startup");
        when(conversationMemory.getRecentMessages("session-startup")).thenReturn(List.of());
        when(policySearchService.searchPolicies("창업 정책", 3, null)).thenReturn(policies);
        when(chatbotAiClient.generateAnswer("창업 정책", List.of(), policies))
                .thenReturn(new ChatbotAiResult("창업 관련 정책을 찾았어요.", List.of()));

        ChatbotAskResponse response = chatbotService.ask("session-startup", "창업 정책");

        assertEquals(ChatbotResponseType.POLICY_SEARCH, response.responseType());
        assertEquals(1, response.matchedPolicyCount());
        verify(chatbotAiClient, never()).classifyIntent(anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void askUsesRecentPolicyKeywordWhenUserAddsAgeContext() {
        List<PolicySearchResultDto> firstPolicies = List.of(
                new PolicySearchResultDto(
                        "svc-startup-1",
                        "Startup Grant",
                        "Supports startup costs.",
                        "Founders",
                        "Grant support",
                        "Online application",
                        "Always open",
                        "https://example.com/policies/svc-startup-1",
                        "Seoul",
                        "Startup Team"
                )
        );
        List<PolicySearchResultDto> ageMatchedPolicies = List.of(
                new PolicySearchResultDto(
                        "svc-startup-50",
                        "Middle-aged Startup Support",
                        "Supports startup costs for middle-aged founders.",
                        "Middle-aged founders",
                        "Startup consulting and grants",
                        "Online application",
                        "Always open",
                        "https://example.com/policies/svc-startup-50",
                        "Seoul",
                        "Startup Team"
                )
        );
        ChatbotConversationMessage previousUserMessage = new ChatbotConversationMessage("user", "창업 지원에 대해 알고 싶어");
        ChatbotConversationMessage previousAssistantMessage = new ChatbotConversationMessage("assistant", "창업 관련 정책을 찾았어요.");

        when(conversationMemory.resolveSessionId("session-startup-age")).thenReturn("session-startup-age");
        when(conversationMemory.getRecentMessages("session-startup-age"))
                .thenReturn(List.of())
                .thenReturn(List.of(previousUserMessage, previousAssistantMessage));
        when(policySearchService.searchPolicies("창업 지원에 대해 알고 싶어", 3, null)).thenReturn(firstPolicies);
        when(policySearchService.searchPolicies("창업 지원에 대해 알고 싶어 중장년 50세", 3, 50)).thenReturn(ageMatchedPolicies);
        when(chatbotAiClient.generateAnswer("창업 지원에 대해 알고 싶어", List.of(), firstPolicies))
                .thenReturn(new ChatbotAiResult("창업 관련 정책을 찾았어요.", List.of()));
        when(chatbotAiClient.generateAnswer("창업 지원에 대해 알고 싶어", List.of(previousUserMessage, previousAssistantMessage), ageMatchedPolicies))
                .thenReturn(new ChatbotAiResult("50세 조건에 맞는 창업 지원 정책을 찾았어요.", List.of()));

        ChatbotAskResponse firstResponse = chatbotService.ask("session-startup-age", "창업 지원에 대해 알고 싶어");
        ChatbotAskResponse secondResponse = chatbotService.ask("session-startup-age", "내 나이가 50세인데 그거에 맞게 찾아줘");

        assertEquals(ChatbotResponseType.POLICY_SEARCH, firstResponse.responseType());
        assertEquals(ChatbotResponseType.POLICY_SEARCH, secondResponse.responseType());
        assertEquals(1, secondResponse.matchedPolicyCount());
        assertEquals("50세 조건에 맞는 창업 지원 정책을 찾았어요.", secondResponse.answer());
        verify(policySearchService).searchPolicies("창업 지원에 대해 알고 싶어 중장년 50세", 3, 50);
    }

    @Test
    void askSearchesStandalonePolicyCategoryKeyword() {
        List<PolicySearchResultDto> policies = List.of(
                new PolicySearchResultDto(
                        "svc-culture",
                        "Culture Voucher",
                        "Supports cultural activity costs.",
                        "Residents",
                        "Voucher support",
                        "Online application",
                        "Always open",
                        "https://example.com/policies/svc-culture",
                        "Seoul",
                        "Culture Team"
                )
        );

        when(conversationMemory.resolveSessionId("session-culture")).thenReturn("session-culture");
        when(conversationMemory.getRecentMessages("session-culture")).thenReturn(List.of());
        when(policySearchService.searchPolicies("문화", 3, null)).thenReturn(policies);
        when(chatbotAiClient.generateAnswer("문화", List.of(), policies))
                .thenReturn(new ChatbotAiResult("문화 관련 정책을 찾았어요.", List.of()));

        ChatbotAskResponse response = chatbotService.ask("session-culture", "문화");

        assertEquals(ChatbotResponseType.POLICY_SEARCH, response.responseType());
        assertEquals(1, response.matchedPolicyCount());
        verify(chatbotAiClient, never()).classifyIntent(anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void askSearchesAgeBenefitQuestionWithAgeFilter() {
        List<PolicySearchResultDto> policies = List.of(
                new PolicySearchResultDto(
                        "svc-middle-age",
                        "Middle-aged Support",
                        "Supports middle-aged residents.",
                        "50 or older",
                        "Benefit support",
                        "Online application",
                        "Always open",
                        "https://example.com/policies/svc-middle-age",
                        "Seoul",
                        "Welfare Team"
                )
        );

        when(conversationMemory.resolveSessionId("session-fifties")).thenReturn("session-fifties");
        when(conversationMemory.getRecentMessages("session-fifties")).thenReturn(List.of());
        when(policySearchService.searchPolicies("혹시 50대 이상이 받을 수 있는 것도 있어? 중장년 50세", 3, 50)).thenReturn(policies);
        when(chatbotAiClient.generateAnswer("혹시 50대 이상이 받을 수 있는 것도 있어?", List.of(), policies))
                .thenReturn(new ChatbotAiResult("50대 이상이 받을 수 있는 정책을 찾았어요.", List.of()));

        ChatbotAskResponse response = chatbotService.ask("session-fifties", "혹시 50대 이상이 받을 수 있는 것도 있어?");

        assertEquals(ChatbotResponseType.POLICY_SEARCH, response.responseType());
        assertEquals(1, response.matchedPolicyCount());
        assertEquals("50대 이상이 받을 수 있는 정책을 찾았어요.", response.answer());
        verify(policySearchService).searchPolicies("혹시 50대 이상이 받을 수 있는 것도 있어? 중장년 50세", 3, 50);
        verify(chatbotAiClient, never()).classifyIntent(anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void askAppliesAgeFilterWhenAgeAndStartupTopicAreInSameMessage() {
        List<PolicySearchResultDto> policies = List.of(
                new PolicySearchResultDto(
                        "svc-startup-50",
                        "중장년 창업 지원",
                        "50대 이상 창업자를 지원합니다.",
                        "50대 이상 예비창업자",
                        "창업 컨설팅 및 사업화 자금 지원",
                        "온라인 신청",
                        "상시",
                        "https://example.com/policies/svc-startup-50",
                        "Seoul",
                        "Startup Team"
                )
        );

        when(conversationMemory.resolveSessionId("session-fifties-startup")).thenReturn("session-fifties-startup");
        when(conversationMemory.getRecentMessages("session-fifties-startup")).thenReturn(List.of());
        when(policySearchService.searchPolicies("50대 이상한테는 창업 관련 정책이 별로 없어? 중장년 50세", 3, 50))
                .thenReturn(policies);
        when(chatbotAiClient.generateAnswer("50대 이상한테는 창업 관련 정책이 별로 없어?", List.of(), policies))
                .thenReturn(new ChatbotAiResult("50대 이상 창업 관련 정책을 찾았어요.", List.of()));

        ChatbotAskResponse response = chatbotService.ask("session-fifties-startup", "50대 이상한테는 창업 관련 정책이 별로 없어?");

        assertEquals(ChatbotResponseType.POLICY_SEARCH, response.responseType());
        assertEquals(1, response.matchedPolicyCount());
        assertEquals("50대 이상 창업 관련 정책을 찾았어요.", response.answer());
        verify(policySearchService).searchPolicies("50대 이상한테는 창업 관련 정책이 별로 없어? 중장년 50세", 3, 50);
        verify(chatbotAiClient, never()).classifyIntent(anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void askUsesRecentPolicyContextWhenUserRequestsMoreResults() {
        List<PolicySearchResultDto> policies = List.of(
                new PolicySearchResultDto(
                        "svc-startup-50",
                        "중장년 창업 지원",
                        "50대 이상 창업자를 지원합니다.",
                        "50대 이상 예비창업자",
                        "창업 컨설팅 및 사업화 자금 지원",
                        "온라인 신청",
                        "상시",
                        "https://example.com/policies/svc-startup-50",
                        "Seoul",
                        "Startup Team"
                )
        );
        ChatbotConversationMessage previousUserMessage = new ChatbotConversationMessage("user", "50대 창업 정책 알려줘");
        ChatbotConversationMessage previousAssistantMessage = new ChatbotConversationMessage("assistant", "창업과 관련된 정책을 찾아봤어요.");

        when(conversationMemory.resolveSessionId("session-more")).thenReturn("session-more");
        when(conversationMemory.getRecentMessages("session-more"))
                .thenReturn(List.of(previousUserMessage, previousAssistantMessage));
        when(policySearchService.searchPolicies("50대 창업 정책 알려줘 중장년 50세", 5, 50))
                .thenReturn(policies);
        when(chatbotAiClient.generateAnswer("50대 창업 정책 알려줘", List.of(previousUserMessage, previousAssistantMessage), policies))
                .thenReturn(new ChatbotAiResult("창업 관련 정책을 더 찾아봤어요.", List.of()));

        ChatbotAskResponse response = chatbotService.ask("session-more", "한 5개만 더 보여줘");

        assertEquals(ChatbotResponseType.POLICY_SEARCH, response.responseType());
        assertEquals(1, response.matchedPolicyCount());
        assertEquals("창업 관련 정책을 더 찾아봤어요.", response.answer());
        verify(policySearchService).searchPolicies("50대 창업 정책 알려줘 중장년 50세", 5, 50);
        verify(chatbotAiClient, never()).classifyIntent(anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.any());
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
    void askTreatsStandaloneKoreanAgeAsProfileContext() {
        when(conversationMemory.resolveSessionId("session-korean-age")).thenReturn("session-korean-age");
        when(conversationMemory.getRecentMessages("session-korean-age"))
                .thenReturn(List.of(new ChatbotConversationMessage("assistant", "청년 지원금, 주거 지원, 취업 지원처럼 말씀해 주세요.")));

        ChatbotAskResponse response = chatbotService.ask("session-korean-age", "내가 지금 스무살이야");

        assertEquals(ChatbotResponseType.CLARIFICATION_NEEDED, response.responseType());
        assertTrue(response.answer().contains("20살이면"));
        assertTrue(response.answer().contains("어떤 분야가 궁금하세요"));
        assertEquals(0, response.matchedPolicyCount());
        assertEquals(4, response.suggestedReplies().size());
        verify(policySearchService, never()).searchPolicies(anyString(), eq(3));
        verify(chatbotAiClient, never()).classifyIntent(anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void askSearchesCategoryAnswerAfterStandaloneAge() {
        List<PolicySearchResultDto> policies = List.of(
                new PolicySearchResultDto(
                        "svc-startup",
                        "Youth Startup Support",
                        "Supports young founders.",
                        "Young founders",
                        "Startup grants",
                        "Online application",
                        "Always open",
                        "https://example.com/policies/svc-startup",
                        "Seoul",
                        "Startup Team"
                )
        );

        when(conversationMemory.resolveSessionId("session-age-category")).thenReturn("session-age-category");
        when(conversationMemory.getRecentMessages("session-age-category"))
                .thenReturn(List.of(new ChatbotConversationMessage("assistant", "청년 지원금, 주거 지원, 취업 지원처럼 말씀해 주세요.")));
        when(policySearchService.searchPolicies("창업 청년 20세", 3, 20)).thenReturn(policies);
        when(chatbotAiClient.generateAnswer("창업", List.of(new ChatbotConversationMessage("assistant", "청년 지원금, 주거 지원, 취업 지원처럼 말씀해 주세요.")), policies))
                .thenReturn(new ChatbotAiResult("청년 창업 지원 정책을 찾았어요.", List.of()));

        ChatbotAskResponse firstResponse = chatbotService.ask("session-age-category", "내가 지금 스무살이야");
        ChatbotAskResponse secondResponse = chatbotService.ask("session-age-category", "창업");

        assertEquals(ChatbotResponseType.CLARIFICATION_NEEDED, firstResponse.responseType());
        assertEquals(ChatbotResponseType.POLICY_SEARCH, secondResponse.responseType());
        assertEquals(1, secondResponse.matchedPolicyCount());
        verify(policySearchService).searchPolicies("창업 청년 20세", 3, 20);
        verify(chatbotAiClient, never()).classifyIntent(anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.any());
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

