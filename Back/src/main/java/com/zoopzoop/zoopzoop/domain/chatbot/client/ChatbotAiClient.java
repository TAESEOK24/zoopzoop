package com.zoopzoop.zoopzoop.domain.chatbot.client;

import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAiResult;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotConversationMessage;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotResponseType;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import java.util.List;

public interface ChatbotAiClient {

    default ChatbotResponseType classifyIntent(
            String userMessage,
            List<ChatbotConversationMessage> history,
            boolean awaitingClarification,
            ChatbotResponseType fallbackType
    ) {
        return null;
    }

    ChatbotAiResult generateAnswer(
            String userMessage,
            List<ChatbotConversationMessage> history,
            List<PolicySearchResultDto> policies
    );
}
