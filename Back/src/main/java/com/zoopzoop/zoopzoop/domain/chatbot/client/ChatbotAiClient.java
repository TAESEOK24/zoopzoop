package com.zoopzoop.zoopzoop.domain.chatbot.client;

import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAiResult;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import java.util.List;

public interface ChatbotAiClient {

    ChatbotAiResult generateAnswer(String userMessage, List<PolicySearchResultDto> policies);
}
