package com.zoopzoop.zoopzoop.domain.chatbot.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAiResult;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotConversationMessage;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotRecommendationDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpenAiChatbotClient implements ChatbotAiClient {

    private static final String FALLBACK_SUMMARY =
            "대화를 이어가며 도와드릴게요. 궁금한 정책 대상이나 상황을 조금 더 말씀해 주세요.";

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiChatbotClient(ObjectProvider<ChatModel> chatModelProvider) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        this.chatClient = chatModel == null ? null : ChatClient.builder(chatModel).build();
    }

    @Override
    public ChatbotAiResult generateAnswer(
            String userMessage,
            List<ChatbotConversationMessage> history,
            List<PolicySearchResultDto> policies
    ) {
        if (chatClient == null) {
            log.warn("ChatModel bean is not available. Using fallback chatbot response.");
            return fallbackAiResult(policies);
        }

        try {
            String content = chatClient.prompt()
                    .system(buildSystemPrompt())
                    .user(buildUserPrompt(userMessage, history, policies))
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                throw new AppException(502, "AI response is empty.");
            }

            return parseAiResult(content, policies);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Spring AI OpenAI request failed", exception);
            throw new AppException(502, "AI response generation failed.");
        }
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

    private ChatbotAiResult parseAiResult(String content, List<PolicySearchResultDto> policies) {
        try {
            ChatbotAiResult result = objectMapper.readValue(content, ChatbotAiResult.class);
            String summary = result.summary();
            if (summary == null || summary.isBlank()) {
                return fallbackAiResult(policies);
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
            return fallbackAiResult(policies);
        }
    }

    private ChatbotAiResult fallbackAiResult(List<PolicySearchResultDto> policies) {
        List<ChatbotRecommendationDto> recommendations = policies.stream()
                .limit(3)
                .map(policy -> new ChatbotRecommendationDto(
                        policy.serviceId(),
                        "질문과 연관도가 높아 우선 확인할 만한 정책입니다."
                ))
                .toList();

        return new ChatbotAiResult(FALLBACK_SUMMARY, recommendations);
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
}
