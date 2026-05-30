package com.zoopzoop.zoopzoop.domain.chatbot.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAiResult;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotConversationMessage;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotRecommendationDto;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotResponseType;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpenAiChatbotClient implements ChatbotAiClient {

    private final ChatClient chatClient;
    private final String classificationModel;
    private final String answerModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiChatbotClient(
            ObjectProvider<ChatModel> chatModelProvider,
            @Value("${chatbot.ai.classification-model:gpt-5.4-nano}") String classificationModel,
            @Value("${chatbot.ai.answer-model:gpt-4o-mini}") String answerModel
    ) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        this.chatClient = chatModel == null ? null : ChatClient.builder(chatModel).build();
        this.classificationModel = classificationModel;
        this.answerModel = answerModel;
    }

    @Override
    public ChatbotResponseType classifyIntent(
            String userMessage,
            List<ChatbotConversationMessage> history,
            boolean awaitingClarification,
            ChatbotResponseType fallbackType
    ) {
        if (chatClient == null) {
            log.warn("ChatModel bean is not available. Using fallback chatbot classification.");
            return fallbackType;
        }

        try {
            String content = chatClient.prompt()
                    .options(OpenAiChatOptions.builder()
                            .model(classificationModel)
                            .temperature(0.0)
                            .build())
                    .system(buildClassificationSystemPrompt())
                    .user(buildClassificationUserPrompt(userMessage, history, awaitingClarification))
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                return fallbackType;
            }

            return parseClassification(content, fallbackType);
        } catch (Exception exception) {
            log.warn("Spring AI OpenAI classification request failed. Using fallback classification.", exception);
            return fallbackType;
        }
    }

    @Override
    public ChatbotAiResult generateAnswer(
            String userMessage,
            List<ChatbotConversationMessage> history,
            List<PolicySearchResultDto> policies
    ) {
        if (chatClient == null) {
            log.warn("ChatModel bean is not available. Using fallback chatbot response.");
            return fallbackAiResult(userMessage, policies);
        }

        try {
            String content = chatClient.prompt()
                    .options(OpenAiChatOptions.builder()
                            .model(answerModel)
                            .build())
                    .system(buildSystemPrompt())
                    .user(buildUserPrompt(userMessage, history, policies))
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                log.warn("Spring AI OpenAI response was empty. Using fallback chatbot response.");
                return fallbackAiResult(userMessage, policies);
            }

            return parseAiResult(content, userMessage, policies);
        } catch (Exception exception) {
            log.warn("Spring AI OpenAI request failed. Using fallback chatbot response.", exception);
            return fallbackAiResult(userMessage, policies);
        }
    }

    private String buildClassificationSystemPrompt() {
        return """
                You classify Korean chatbot user messages for a welfare-policy assistant.
                Return JSON only with this schema:
                {
                  "responseType": "POLICY_SEARCH | CLARIFICATION_NEEDED | SMALLTALK | OFF_TOPIC | SAFETY"
                }

                Choose SAFETY for self-harm, suicide, violence, medical emergency, or urgent danger.
                Choose CLARIFICATION_NEEDED for ambiguous hardship where more user profile information is needed.
                Choose POLICY_SEARCH for welfare, housing, employment, youth, living-cost, benefit, or application questions.
                Choose SMALLTALK for greetings, thanks, or casual conversation.
                Choose OFF_TOPIC when unrelated to welfare-policy assistance.
                If awaitingClarification is true and the user provides a short profile answer, choose CLARIFICATION_NEEDED.
                Do not include markdown.
                """;
    }

    private String buildClassificationUserPrompt(
            String userMessage,
            List<ChatbotConversationMessage> history,
            boolean awaitingClarification
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("awaitingClarification=").append(awaitingClarification).append('\n');
        builder.append("Recent conversation:\n");

        if (history.isEmpty()) {
            builder.append("- no previous messages\n");
        } else {
            history.forEach(message -> builder.append("- ")
                    .append(message.role())
                    .append(": ")
                    .append(trimToLength(message.content(), 100))
                    .append('\n'));
        }

        builder.append("\nCurrent user message:\n")
                .append(userMessage)
                .append("\n\nReturn compact JSON only.");
        return builder.toString();
    }

    private String buildSystemPrompt() {
        return """
                You are a Korean welfare policy chat assistant.
                Continue the conversation naturally using the recent chat history.
                If policy candidates are provided, briefly summarize them and explain why they fit.
                If no policy candidates are provided, answer conversationally and ask a short follow-up question when helpful.
                Keep the response concise.
                Return JSON only with this schema:
                {
                  "summary": "short Korean reply",
                  "recommendations": [
                    {
                      "serviceId": "policy id",
                      "reason": "one short Korean sentence"
                    }
                  ]
                }
                Do not include markdown.
                Do not return more than 3 recommendations.
                """;
    }

    private String buildUserPrompt(
            String userMessage,
            List<ChatbotConversationMessage> history,
            List<PolicySearchResultDto> policies
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("Recent conversation:\n");

        if (history.isEmpty()) {
            builder.append("- no previous messages\n");
        } else {
            history.forEach(message -> builder.append("- ")
                    .append(message.role())
                    .append(": ")
                    .append(trimToLength(message.content(), 140))
                    .append('\n'));
        }

        builder.append("\nCurrent user message:\n")
                .append(userMessage)
                .append("\n\nPolicy candidates:\n");

        if (policies.isEmpty()) {
            builder.append("- none\n");
        } else {
            for (int i = 0; i < policies.size(); i++) {
                PolicySearchResultDto policy = policies.get(i);
                builder.append(i + 1).append(". serviceId=").append(nullSafe(policy.serviceId())).append('\n')
                        .append("   serviceName=").append(nullSafe(policy.serviceName())).append('\n')
                        .append("   purposeSummary=").append(trimToLength(policy.purposeSummary(), 120)).append('\n')
                        .append("   target=").append(trimToLength(policy.target(), 120)).append('\n')
                        .append("   supportContent=").append(trimToLength(policy.supportContent(), 120)).append('\n')
                        .append("   applicationMethod=").append(trimToLength(policy.applicationMethod(), 80)).append('\n');
            }
        }

        builder.append("\nReturn compact JSON only.");
        return builder.toString();
    }

    private ChatbotResponseType parseClassification(String content, ChatbotResponseType fallbackType) {
        try {
            ClassificationResult result = objectMapper.readValue(content, ClassificationResult.class);
            if (result.responseType() == null || result.responseType().isBlank()) {
                return fallbackType;
            }
            return ChatbotResponseType.valueOf(result.responseType().trim());
        } catch (Exception parseException) {
            log.warn("Failed to parse AI classification as JSON, using fallback classification");
            return fallbackType;
        }
    }

    private ChatbotAiResult parseAiResult(String content, String userMessage, List<PolicySearchResultDto> policies) {
        try {
            ChatbotAiResult result = objectMapper.readValue(content, ChatbotAiResult.class);
            String summary = result.summary();
            if (summary == null || summary.isBlank()) {
                return fallbackAiResult(userMessage, policies);
            }

            List<ChatbotRecommendationDto> recommendations = result.recommendations() == null
                    ? List.of()
                    : result.recommendations().stream()
                            .filter(recommendation -> recommendation.serviceId() != null && !recommendation.serviceId().isBlank())
                            .map(recommendation -> new ChatbotRecommendationDto(
                                    recommendation.serviceId().trim(),
                                    sanitizeReason(recommendation.reason())
                            ))
                            .limit(3)
                            .toList();

            return new ChatbotAiResult(summary.trim(), recommendations);
        } catch (Exception parseException) {
            log.warn("Failed to parse AI response as JSON, using fallback summary");
            return fallbackAiResult(userMessage, policies);
        }
    }

    private ChatbotAiResult fallbackAiResult(String userMessage, List<PolicySearchResultDto> policies) {
        List<ChatbotRecommendationDto> recommendations = policies.stream()
                .limit(3)
                .map(policy -> new ChatbotRecommendationDto(
                        policy.serviceId(),
                        "질문과 연관도가 높아 우선 확인할 만한 정책입니다."
                ))
                .toList();

        return new ChatbotAiResult(fallbackSummary(userMessage, policies), recommendations);
    }

    private String fallbackSummary(String userMessage, List<PolicySearchResultDto> policies) {
        String message = userMessage == null ? "" : userMessage.trim();
        if (policies.isEmpty()) {
            return "조건에 맞는 정책을 바로 찾지 못했어요. 대상이나 상황을 조금 더 구체적으로 알려주시면 다시 찾아볼게요.";
        }
        if (message.contains("50대 이상")) {
            return "네, 50대 이상이 받을 수 있는 정책을 찾아봤어요.";
        }
        if (message.contains("50대") || message.contains("50세")) {
            return "네, 50대 조건에 맞는 정책을 찾아봤어요.";
        }
        if (message.contains("창업")) {
            return "네, 창업과 관련된 정책을 찾아봤어요.";
        }
        if (message.contains("주거") || message.contains("월세") || message.contains("전세")) {
            return "네, 주거와 관련된 정책을 찾아봤어요.";
        }
        if (message.contains("취업") || message.contains("구직") || message.contains("일자리")) {
            return "네, 취업과 관련된 정책을 찾아봤어요.";
        }
        return "네, 질문과 관련된 정책을 찾아봤어요.";
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String trimToLength(String value, int maxLength) {
        String sanitized = nullSafe(value);
        if (sanitized.length() <= maxLength) {
            return sanitized;
        }
        return sanitized.substring(0, maxLength) + "...";
    }

    private String sanitizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "질문과 연관된 정책입니다.";
        }
        return reason.trim();
    }

    private record ClassificationResult(String responseType) {
    }
}
