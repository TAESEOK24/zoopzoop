package com.zoopzoop.zoopzoop.domain.chatbot.service;

import com.zoopzoop.zoopzoop.domain.chatbot.client.ChatbotAiClient;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAiResult;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAskResponse;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotConversationMessage;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotPolicyDto;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotRecommendationDto;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotReferenceDto;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotResponseType;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotSuggestedReplyDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySearchService;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final int SEARCH_SIZE = 3;
    private static final Pattern AGE_PATTERN = Pattern.compile("(?:만\\s*)?(\\d{1,3})\\s*(?:세|살)");
    private static final Pattern AGE_CONTEXT_PATTERN = Pattern.compile("(?:나이|연령)\\D{0,8}(\\d{1,3})");
    private static final Pattern AGE_DECADE_PATTERN = Pattern.compile("(\\d{2,3})\\s*대");
    private static final Pattern KOREAN_AGE_PATTERN = Pattern.compile(
            "(스물아홉|스물여덟|스물일곱|스물여섯|스물다섯|스물넷|스물네|스물셋|스물세|스물둘|스물두|스물한|스물하나|스무|스물)\\s*(?:세|살)"
    );
    private static final Pattern ONLY_NUMBER_PATTERN = Pattern.compile("\\d{1,3}");
    private static final Pattern REQUESTED_COUNT_PATTERN = Pattern.compile("(\\d{1,2})\\s*개");

    private final PolicySearchService policySearchService;
    private final ChatbotAiClient chatbotAiClient;
    private final ChatbotConversationMemory conversationMemory;
    private final ChatbotIntentClassifier intentClassifier;
    private final ChatbotIntakeMemory intakeMemory;

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("chatbot", "chatbot module ready");
    }

    public ChatbotAskResponse ask(String sessionId, String message) {
        String resolvedSessionId = conversationMemory.resolveSessionId(sessionId);
        List<ChatbotConversationMessage> history = conversationMemory.getRecentMessages(resolvedSessionId);
        ChatbotIntakeMemory.ChatbotIntakeProfile profile = intakeMemory.getProfile(resolvedSessionId);

        ChatbotAskResponse deterministicResponse = buildDeterministicResponse(resolvedSessionId, history, message, profile);
        if (deterministicResponse != null) {
            conversationMemory.appendUserMessage(resolvedSessionId, message);
            conversationMemory.appendAssistantMessage(resolvedSessionId, deterministicResponse.answer());
            return deterministicResponse;
        }

        ChatbotResponseType fallbackResponseType = intentClassifier.classify(message, profile.isAwaitingClarification());
        ChatbotResponseType aiResponseType = chatbotAiClient.classifyIntent(
                message,
                history,
                profile.isAwaitingClarification(),
                fallbackResponseType
        );
        ChatbotResponseType responseType = aiResponseType == null ? fallbackResponseType : aiResponseType;

        ChatbotAskResponse response = switch (responseType) {
            case POLICY_SEARCH -> buildPolicySearchResponse(resolvedSessionId, history, message, ChatbotResponseType.POLICY_SEARCH);
            case CLARIFICATION_NEEDED -> buildClarificationResponse(resolvedSessionId, message, history);
            case SMALLTALK -> buildSimpleResponse(
                    resolvedSessionId,
                    "안녕하세요. 복지 정책과 지원 제도를 안내해드릴 수 있어요. 예를 들면 청년 지원금, 주거 지원, 취업 지원처럼 말씀해 주세요.",
                    ChatbotResponseType.SMALLTALK,
                    defaultPolicySuggestions()
            );
            case OFF_TOPIC -> buildSimpleResponse(
                    resolvedSessionId,
                    "저는 복지 정책 안내 챗봇이에요. 청년, 주거, 취업, 돌봄, 긴급복지 같은 질문을 주시면 더 정확하게 도와드릴 수 있어요.",
                    ChatbotResponseType.OFF_TOPIC,
                    defaultPolicySuggestions()
            );
            case SAFETY -> buildSimpleResponse(
                    resolvedSessionId,
                    "지금은 정책 안내보다 즉시 도움을 받는 게 우선일 수 있어요. 응급 상황이면 119나 가까운 응급실로 바로 연락해 주세요. 심리적으로 매우 힘든 상태라면 자살예방상담전화 109 또는 정신건강상담 1577-0199에 즉시 도움을 요청해 주세요.",
                    ChatbotResponseType.SAFETY,
                    List.of(
                            new ChatbotSuggestedReplyDto("긴급복지 문의", "긴급복지 지원 알려줘"),
                            new ChatbotSuggestedReplyDto("생계 지원 문의", "생계 지원 정책 알려줘")
                    )
            );
        };

        conversationMemory.appendUserMessage(resolvedSessionId, message);
        conversationMemory.appendAssistantMessage(resolvedSessionId, response.answer());
        return response;
    }

    private ChatbotAskResponse buildDeterministicResponse(
            String sessionId,
            List<ChatbotConversationMessage> history,
            String message,
            ChatbotIntakeMemory.ChatbotIntakeProfile profile
    ) {
        ChatbotResponseType fallbackResponseType = intentClassifier.classify(message, profile.isAwaitingClarification());
        if (fallbackResponseType == ChatbotResponseType.SAFETY) {
            return null;
        }

        String recentPolicyContext = findRecentPolicyContext(history);
        if (recentPolicyContext != null && isMorePolicyRequest(message)) {
            return buildPolicySearchResponse(
                    sessionId,
                    history,
                    recentPolicyContext,
                    ChatbotResponseType.POLICY_SEARCH,
                    extractRequestedPolicyCount(message)
            );
        }

        if (isSpecificPolicySearchQuestion(message) || isPolicyCategoryQuestion(message) || isApplicationConditionQuestion(message)) {
            if (extractAge(message == null ? "" : message.trim().toLowerCase(), false) != null) {
                intakeMemory.updateProfile(sessionId, existing -> {
                    mergeProfile(existing, message);
                    return existing;
                });
            }
            return buildPolicySearchResponse(sessionId, history, message, ChatbotResponseType.POLICY_SEARCH);
        }

        if (hasProfileContext(profile) && isPolicyCategoryAnswer(message)) {
            return buildPolicySearchResponse(sessionId, history, message, ChatbotResponseType.POLICY_SEARCH);
        }

        if (!profile.isAwaitingClarification()
                && recentPolicyContext != null
                && isProfileContextOnlyMessage(message)) {
            intakeMemory.updateProfile(sessionId, existing -> {
                mergeProfile(existing, message);
                return existing;
            });
            return buildPolicySearchResponse(sessionId, history, recentPolicyContext, ChatbotResponseType.POLICY_SEARCH);
        }

        if (isAgeBenefitSearchQuestion(message)) {
            intakeMemory.updateProfile(sessionId, existing -> {
                mergeProfile(existing, message);
                return existing;
            });
            return buildPolicySearchResponse(sessionId, history, message, ChatbotResponseType.POLICY_SEARCH);
        }

        if (isBroadPolicyClarificationQuestion(message)) {
            return buildPolicyCategoryClarificationResponse(sessionId);
        }

        if (!profile.isAwaitingClarification() && isProfileContextOnlyMessage(message)) {
            return buildProfileContextFollowUpResponse(sessionId, message);
        }

        if (isBroadBenefitClarificationQuestion(message)
                || (isHardshipClarificationQuestion(message) && !isSpecificPolicySearchQuestion(message))
                || (profile.isAwaitingClarification() && isLikelyProfileAnswer(message))) {
            return buildClarificationResponse(sessionId, message, history);
        }

        return null;
    }

    private ChatbotAskResponse buildPolicySearchResponse(
            String sessionId,
            List<ChatbotConversationMessage> history,
            String message,
            ChatbotResponseType responseType
    ) {
        return buildPolicySearchResponse(sessionId, history, message, responseType, SEARCH_SIZE);
    }

    private ChatbotAskResponse buildPolicySearchResponse(
            String sessionId,
            List<ChatbotConversationMessage> history,
            String message,
            ChatbotResponseType responseType,
            int searchSize
    ) {
        ChatbotIntakeMemory.ChatbotIntakeProfile profile = intakeMemory.getProfile(sessionId);
        String normalizedMessage = message == null ? "" : message.trim().toLowerCase();
        Integer effectiveAge = profile.age() != null ? profile.age() : extractAge(normalizedMessage, false);
        String effectiveAgeGroup = profile.ageGroup() != null
                ? profile.ageGroup()
                : effectiveAge != null ? resolveAgeGroup(effectiveAge) : null;
        String query = buildSearchQuery(message, profile, effectiveAge, effectiveAgeGroup);
        List<PolicySearchResultDto> policies = policySearchService.searchPolicies(query, searchSize, effectiveAge);
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

        intakeMemory.clear(sessionId);

        return new ChatbotAskResponse(
                sessionId,
                aiResult.summary(),
                responseType,
                buildFilterSuggestions(message),
                policyCards,
                references,
                policies.size()
        );
    }

    private ChatbotAskResponse buildClarificationResponse(
            String sessionId,
            String message,
            List<ChatbotConversationMessage> history
    ) {
        ChatbotIntakeMemory.ChatbotIntakeProfile profile = intakeMemory.updateProfile(sessionId, existing -> {
            mergeProfile(existing, message);
            if (existing.concernMessage() == null || looksLikeConcernMessage(message)) {
                existing.concernMessage(message);
            }
            return existing;
        });

        if (hasMinimumSearchProfile(profile) && profile.concernMessage() != null) {
            return buildPolicySearchResponse(sessionId, history, profile.concernMessage(), ChatbotResponseType.POLICY_SEARCH);
        }

        String nextField = nextMissingField(profile);
        profile.awaitingField(nextField);

        String answer = profile.completedFieldCount() == 0
                ? "생활이 많이 부담되실 수 있겠어요. 맞는 지원 정책을 찾으려면 몇 가지만 알려주세요. " + questionFor(nextField)
                : "알겠습니다. 더 맞는 정책을 찾으려면 " + questionFor(nextField);

        return new ChatbotAskResponse(
                sessionId,
                answer,
                ChatbotResponseType.CLARIFICATION_NEEDED,
                suggestionsFor(nextField),
                List.of(),
                List.of(),
                0
        );
    }

    private ChatbotAskResponse buildSimpleResponse(
            String sessionId,
            String answer,
            ChatbotResponseType responseType,
            List<ChatbotSuggestedReplyDto> suggestedReplies
    ) {
        return new ChatbotAskResponse(
                sessionId,
                answer,
                responseType,
                suggestedReplies,
                List.of(),
                List.of(),
                0
        );
    }

    private ChatbotAskResponse buildPolicyCategoryClarificationResponse(String sessionId) {
        return new ChatbotAskResponse(
                sessionId,
                "정책 범위가 넓어요. 주거, 취업, 생활비, 창업 중 어떤 분야가 궁금하세요?",
                ChatbotResponseType.CLARIFICATION_NEEDED,
                List.of(
                        new ChatbotSuggestedReplyDto("주거 지원", "청년 주거 지원 정책 알려줘"),
                        new ChatbotSuggestedReplyDto("취업 지원", "청년 취업 지원 정책 알려줘"),
                        new ChatbotSuggestedReplyDto("생활비 지원", "청년 생활비 지원 정책 알려줘"),
                        new ChatbotSuggestedReplyDto("창업 지원", "청년 창업 지원 정책 알려줘")
                ),
                List.of(),
                List.of(),
                0
        );
    }

    private ChatbotAskResponse buildProfileContextFollowUpResponse(String sessionId, String message) {
        ChatbotIntakeMemory.ChatbotIntakeProfile profile = intakeMemory.updateProfile(sessionId, existing -> {
            mergeProfile(existing, message);
            return existing;
        });

        String agePrefix = profile.age() != null
                ? profile.age() + "살이면 "
                : profile.ageGroup() != null ? profile.ageGroup() + "이라면 " : "";

        return new ChatbotAskResponse(
                sessionId,
                agePrefix + "관련 지원을 더 잘 찾을 수 있어요. 주거, 취업, 생활비, 창업 중 어떤 분야가 궁금하세요?",
                ChatbotResponseType.CLARIFICATION_NEEDED,
                List.of(
                        new ChatbotSuggestedReplyDto("주거 지원", "청년 주거 지원 정책 알려줘"),
                        new ChatbotSuggestedReplyDto("취업 지원", "청년 취업 지원 정책 알려줘"),
                        new ChatbotSuggestedReplyDto("생활비 지원", "청년 생활비 지원 정책 알려줘"),
                        new ChatbotSuggestedReplyDto("창업 지원", "청년 창업 지원 정책 알려줘")
                ),
                List.of(),
                List.of(),
                0
        );
    }

    private void mergeProfile(ChatbotIntakeMemory.ChatbotIntakeProfile profile, String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage.trim().toLowerCase();

        Integer age = extractAge(message, "ageGroup".equals(profile.awaitingField()));
        if (age != null) {
            profile.age(age);
            profile.ageGroup(resolveAgeGroup(age));
        } else if (containsAny(message, "청년", "20대", "30대 초반", "대학생")) {
            profile.ageGroup("청년");
        } else if (containsAny(message, "중장년", "40대", "50대", "장년")) {
            profile.ageGroup("중장년");
        } else if (containsAny(message, "노년", "어르신", "고령", "65세 이상", "70대", "80대")) {
            profile.ageGroup("노년");
        }

        if (containsAny(message, "혼자", "1인 가구", "자취")) {
            profile.householdType("1인 가구");
        } else if (containsAny(message, "가족", "부모", "배우자", "아이와")) {
            profile.householdType("가족 동거");
        }

        if (containsAny(message, "구직 중", "취업 준비", "취준", "일자리 찾")) {
            profile.employmentStatus("구직 중");
        } else if (containsAny(message, "무직", "실직", "백수")) {
            profile.employmentStatus("무직");
        } else if (containsAny(message, "일하고", "재직", "직장", "근무")) {
            profile.employmentStatus("재직 중");
        }

        if (containsAny(message, "월세")) {
            profile.housingStatus("월세 거주");
        } else if (containsAny(message, "전세")) {
            profile.housingStatus("전세 거주");
        }
    }

    private Integer extractAge(String message, boolean awaitingAgeAnswer) {
        Integer explicitAge = extractAgeWithPattern(AGE_PATTERN, message);
        if (explicitAge != null) {
            return explicitAge;
        }

        Integer contextualAge = extractAgeWithPattern(AGE_CONTEXT_PATTERN, message);
        if (contextualAge != null) {
            return contextualAge;
        }

        Integer decadeAge = extractAgeWithPattern(AGE_DECADE_PATTERN, message);
        if (decadeAge != null) {
            return decadeAge;
        }

        Integer koreanAge = extractKoreanAge(message);
        if (koreanAge != null) {
            return koreanAge;
        }

        if (awaitingAgeAnswer && ONLY_NUMBER_PATTERN.matcher(message).matches()) {
            int age = Integer.parseInt(message);
            return isValidAge(age) ? age : null;
        }

        return null;
    }

    private Integer extractKoreanAge(String message) {
        Matcher matcher = KOREAN_AGE_PATTERN.matcher(message);
        while (matcher.find()) {
            Integer age = koreanAgeWordToNumber(matcher.group(1));
            if (age != null && isValidAge(age)) {
                return age;
            }
        }
        return null;
    }

    private Integer koreanAgeWordToNumber(String word) {
        return switch (word) {
            case "스무", "스물" -> 20;
            case "스물하나", "스물한" -> 21;
            case "스물둘", "스물두" -> 22;
            case "스물셋", "스물세" -> 23;
            case "스물넷", "스물네" -> 24;
            case "스물다섯" -> 25;
            case "스물여섯" -> 26;
            case "스물일곱" -> 27;
            case "스물여덟" -> 28;
            case "스물아홉" -> 29;
            default -> null;
        };
    }

    private Integer extractAgeWithPattern(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            int age = Integer.parseInt(matcher.group(1));
            if (isValidAge(age)) {
                return age;
            }
        }
        return null;
    }

    private boolean isValidAge(int age) {
        return age > 0 && age <= 120;
    }

    private String resolveAgeGroup(int age) {
        if (age < 19) {
            return "청소년";
        }
        if (age <= 39) {
            return "청년";
        }
        if (age <= 64) {
            return "중장년";
        }
        return "노년";
    }

    private String buildSearchQuery(String message, ChatbotIntakeMemory.ChatbotIntakeProfile profile) {
        return buildSearchQuery(message, profile, profile.age(), profile.ageGroup());
    }

    private String buildSearchQuery(
            String message,
            ChatbotIntakeMemory.ChatbotIntakeProfile profile,
            Integer effectiveAge,
            String effectiveAgeGroup
    ) {
        List<String> tokens = new ArrayList<>();
        tokens.add(message);

        if (effectiveAgeGroup != null) {
            tokens.add(effectiveAgeGroup);
        }
        if (effectiveAge != null) {
            tokens.add(effectiveAge + "세");
        }
        if (profile.householdType() != null) {
            tokens.add(profile.householdType());
        }
        if (profile.employmentStatus() != null) {
            tokens.add(profile.employmentStatus());
        }
        if (profile.housingStatus() != null) {
            tokens.add(profile.housingStatus());
        }

        return tokens.stream()
                .filter(token -> token != null && !token.isBlank())
                .distinct()
                .collect(Collectors.joining(" "));
    }

    private String nextMissingField(ChatbotIntakeMemory.ChatbotIntakeProfile profile) {
        if (profile.ageGroup() == null) {
            return "ageGroup";
        }
        if (profile.householdType() == null) {
            return "householdType";
        }
        if (profile.employmentStatus() == null) {
            return "employmentStatus";
        }
        return "housingStatus";
    }

    private String questionFor(String field) {
        return switch (field) {
            case "ageGroup" -> "현재 연령대가 어떻게 되시나요?";
            case "householdType" -> "혼자 살고 계신가요, 가족과 함께 살고 계신가요?";
            case "employmentStatus" -> "현재 일하고 계신가요, 아니면 구직 중이신가요?";
            default -> "현재 월세, 전세, 자가 중 어떤 주거 형태이신가요?";
        };
    }

    private List<ChatbotSuggestedReplyDto> suggestionsFor(String field) {
        return switch (field) {
            case "ageGroup" -> List.of(
                    new ChatbotSuggestedReplyDto("청년", "청년"),
                    new ChatbotSuggestedReplyDto("중장년", "중장년")
            );
            case "householdType" -> List.of(
                    new ChatbotSuggestedReplyDto("혼자 거주", "혼자 살아요"),
                    new ChatbotSuggestedReplyDto("가족과 거주", "가족과 살아요")
            );
            case "employmentStatus" -> List.of(
                    new ChatbotSuggestedReplyDto("구직 중", "구직 중이에요"),
                    new ChatbotSuggestedReplyDto("무직", "무직이에요"),
                    new ChatbotSuggestedReplyDto("재직 중", "현재 일하고 있어요")
            );
            default -> List.of(
                    new ChatbotSuggestedReplyDto("월세 거주", "월세에 살아요"),
                    new ChatbotSuggestedReplyDto("전세 거주", "전세에 살아요")
            );
        };
    }

    private List<ChatbotSuggestedReplyDto> defaultPolicySuggestions() {
        return List.of(
                new ChatbotSuggestedReplyDto("청년 지원", "청년 지원 정책 알려줘"),
                new ChatbotSuggestedReplyDto("주거 지원", "주거 지원 정책 알려줘"),
                new ChatbotSuggestedReplyDto("취업 지원", "취업 지원 정책 알려줘")
        );
    }

    private List<ChatbotSuggestedReplyDto> buildFilterSuggestions(String message) {
        return List.of(
                new ChatbotSuggestedReplyDto("주거 지원만 보기", message + " 주거 지원만 다시 보여줘"),
                new ChatbotSuggestedReplyDto("청년 대상만 보기", message + " 청년 대상만 추려줘"),
                new ChatbotSuggestedReplyDto("신청 조건 보기", "신청 조건 더 자세히 알려줘")
        );
    }

    private boolean looksLikeConcernMessage(String message) {
        return message != null && message.length() >= 8;
    }

    private boolean hasMinimumSearchProfile(ChatbotIntakeMemory.ChatbotIntakeProfile profile) {
        return profile.ageGroup() != null && profile.householdType() != null;
    }

    private boolean hasProfileContext(ChatbotIntakeMemory.ChatbotIntakeProfile profile) {
        return profile.ageGroup() != null
                || profile.age() != null
                || profile.householdType() != null
                || profile.employmentStatus() != null
                || profile.housingStatus() != null;
    }

    private String findRecentPolicyContext(List<ChatbotConversationMessage> history) {
        for (int index = history.size() - 1; index >= 0; index--) {
            ChatbotConversationMessage message = history.get(index);
            if (!"user".equals(message.role())) {
                continue;
            }

            String content = message.content();
            if (isSpecificPolicySearchQuestion(content) || isPolicyCategoryQuestion(content)) {
                return content;
            }
        }
        return null;
    }

    private boolean isBroadPolicyClarificationQuestion(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.trim().toLowerCase();
        boolean broadYouthQuestion = normalized.contains("청년")
                && containsAny(normalized, "정책", "지원", "혜택", "복지")
                && !containsAny(
                        normalized,
                        "주거", "월세", "전세", "자가", "보증금",
                        "취업", "구직", "일자리", "실업",
                        "생활비", "생계", "긴급", "지원금", "수당",
                        "창업", "사업", "돌봄", "출산", "육아", "대출", "신청", "조건", "대상"
                );

        boolean broadWelfareQuestion = containsAny(normalized, "복지 지원", "지원 정책", "받을 수 있는 혜택")
                && !containsAny(
                        normalized,
                        "주거", "월세", "전세", "자가", "보증금",
                        "취업", "구직", "일자리", "실업",
                        "생활비", "생계", "긴급", "지원금", "수당",
                        "창업", "사업", "돌봄", "출산", "육아", "대출", "신청", "조건", "대상"
                );

        return broadYouthQuestion || broadWelfareQuestion;
    }

    private boolean isHardshipClarificationQuestion(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.trim().toLowerCase();
        return containsAny(
                normalized,
                "가난", "생활이 힘들", "너무 힘들", "돈이 없", "월세 내기", "월세 내기가",
                "생활비가", "생활비 부족", "주거비", "생계가 어렵", "먹고살기 힘들", "버티기 힘들"
        );
    }

    private boolean isBroadBenefitClarificationQuestion(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.trim().toLowerCase();
        return containsAny(normalized, "받을 수 있는 지원금", "우리가 받을 수 있는 지원금", "받을 수 있는 지원")
                && !containsAny(normalized, "청년 주거", "청년 취업", "청년 창업", "청년 생활비", "긴급복지");
    }

    private boolean isSpecificPolicySearchQuestion(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.trim().toLowerCase();
        return containsAny(
                normalized,
                "구직 지원", "구직 중인데 받을 수 있는 지원", "취업 지원", "일자리 지원",
                "청년 주거", "청년 월세", "청년 취업", "청년 창업", "청년 생활비", "긴급복지"
        );
    }

    private boolean isPolicyCategoryQuestion(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.trim().toLowerCase();
        return isPolicyCategoryAnswer(normalized)
                && (normalized.length() <= 20 || containsAny(normalized, "정책", "지원", "혜택", "추천", "알려", "찾아", "있어", "궁금"));
    }

    private boolean isApplicationConditionQuestion(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.trim().toLowerCase();
        return containsAny(normalized, "신청 조건", "신청 자격", "지원 조건", "대상 조건");
    }

    private boolean isMorePolicyRequest(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.trim().toLowerCase();
        return containsAny(normalized, "더 보여", "더 찾아", "더 추천", "추가", "다른", "또", "더 있어", "더 알려")
                || REQUESTED_COUNT_PATTERN.matcher(normalized).find();
    }

    private int extractRequestedPolicyCount(String message) {
        if (message == null) {
            return SEARCH_SIZE;
        }

        Matcher matcher = REQUESTED_COUNT_PATTERN.matcher(message);
        if (!matcher.find()) {
            return SEARCH_SIZE;
        }

        int requestedCount = Integer.parseInt(matcher.group(1));
        return Math.max(1, Math.min(requestedCount, 10));
    }

    private boolean isAgeBenefitSearchQuestion(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.trim().toLowerCase();
        return extractAge(normalized, false) != null
                && containsAny(normalized, "받을 수", "가능", "대상", "정책", "지원", "혜택", "추천", "찾아", "알려", "있어");
    }

    private boolean isPolicyCategoryAnswer(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.trim().toLowerCase();
        return containsAny(
                normalized,
                "주거", "월세", "전세",
                "취업", "구직", "일자리",
                "생활비", "생계", "지원금",
                "창업", "사업",
                "돌봄", "출산", "육아", "임신", "보육",
                "교육", "문화", "교통",
                "의료", "건강", "장애", "다문화",
                "긴급복지", "대출", "보증금"
        );
    }

    private boolean isProfileContextOnlyMessage(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.trim().toLowerCase();
        return extractAge(normalized, false) != null
                || containsAny(normalized, "청년", "20대", "30대 초반", "대학생", "중장년", "40대", "50대", "장년", "노년", "어르신", "고령");
    }

    private boolean isLikelyProfileAnswer(String message) {
        return message != null && message.trim().length() <= 30;
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
