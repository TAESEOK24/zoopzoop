package com.zoopzoop.zoopzoop.domain.chatbot.client;

import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpenAiChatbotClient implements ChatbotAiClient {

    private final ChatClient chatClient;

    public OpenAiChatbotClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String generateAnswer(String userMessage, List<PolicySearchResultDto> policies) {
        try {
            String content = chatClient.prompt()
                    .system(buildSystemPrompt())
                    .user(buildUserPrompt(userMessage, policies))
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                throw new AppException(502, "AI 응답 내용이 비어 있습니다.");
            }

            return content.trim();
        } catch (Exception exception) {
            log.error("Spring AI OpenAI request failed", exception);
            throw new AppException(502, "AI 응답 생성 중 오류가 발생했습니다.");
        }
    }

    private String buildSystemPrompt() {
        return """
                당신은 복지 정책 안내 챗봇입니다.
                반드시 제공된 정책 검색 결과만 근거로 답변하세요.
                검색 결과에 없는 내용은 추측하지 마세요.
                답변은 한국어로 작성하세요.
                정책명, 지원 내용, 대상, 신청 방법을 간단히 정리하세요.
                """;
    }

    private String buildUserPrompt(String userMessage, List<PolicySearchResultDto> policies) {
        StringBuilder builder = new StringBuilder();
        builder.append("사용자 질문:\n")
                .append(userMessage)
                .append("\n\n")
                .append("정책 검색 결과:\n");

        for (int i = 0; i < policies.size(); i++) {
            PolicySearchResultDto policy = policies.get(i);
            builder.append(i + 1).append(". 정책명: ").append(nullSafe(policy.serviceName())).append('\n')
                    .append("   서비스 ID: ").append(nullSafe(policy.serviceId())).append('\n')
                    .append("   요약: ").append(nullSafe(policy.purposeSummary())).append('\n')
                    .append("   대상: ").append(nullSafe(policy.target())).append('\n')
                    .append("   지원 내용: ").append(nullSafe(policy.supportContent())).append('\n')
                    .append("   신청 방법: ").append(nullSafe(policy.applicationMethod())).append('\n')
                    .append("   상세 링크: ").append(nullSafe(policy.detailUrl())).append('\n');
        }

        builder.append("\n위 정책만 근거로 답변하고, 관련 정책이 없다면 그 사실만 명확히 말해 주세요.");
        return builder.toString();
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
