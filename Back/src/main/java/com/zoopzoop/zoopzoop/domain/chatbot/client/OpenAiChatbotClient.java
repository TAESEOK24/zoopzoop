package com.zoopzoop.zoopzoop.domain.chatbot.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class OpenAiChatbotClient implements ChatbotAiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-5.4-nano}")
    private String model;

    @Override
    public String generateAnswer(String userMessage, List<PolicySearchResultDto> policies) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AppException(503, "OPENAI_API_KEY가 설정되지 않았습니다.");
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(
                        new Message("system", buildSystemPrompt()),
                        new Message("user", buildUserPrompt(userMessage, policies))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(
                    baseUrl + "/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    ChatCompletionResponse.class
            );

            ChatCompletionResponse body = response.getBody();
            if (body == null || body.choices() == null || body.choices().isEmpty()) {
                throw new AppException(502, "AI 응답을 해석할 수 없습니다.");
            }

            Choice choice = body.choices().get(0);
            if (choice.message() == null || choice.message().content() == null || choice.message().content().isBlank()) {
                throw new AppException(502, "AI 응답 내용이 비어 있습니다.");
            }

            return choice.message().content().trim();
        } catch (RestClientException exception) {
            log.error("OpenAI request failed", exception);
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

    private record ChatCompletionRequest(
            String model,
            List<Message> messages
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponse(
            List<Choice> choices
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(
            Message message
    ) {
    }

    private record Message(
            String role,
            String content
    ) {
    }
}
