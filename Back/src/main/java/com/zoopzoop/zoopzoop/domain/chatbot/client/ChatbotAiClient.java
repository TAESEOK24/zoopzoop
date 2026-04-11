package com.zoopzoop.zoopzoop.domain.chatbot.client;

import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import java.util.List;

public interface ChatbotAiClient {

    String generateAnswer(String userMessage, List<PolicySearchResultDto> policies);
}
