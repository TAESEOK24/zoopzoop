package com.zoopzoop.zoopzoop.domain.chatbot.service;

import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotConversationMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ChatbotConversationMemory {

    private static final int MAX_MESSAGES = 12;

    private final Map<String, List<ChatbotConversationMessage>> conversations = new ConcurrentHashMap<>();

    public String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId.trim();
    }

    public List<ChatbotConversationMessage> getRecentMessages(String sessionId) {
        return List.copyOf(conversations.getOrDefault(sessionId, List.of()));
    }

    public void appendUserMessage(String sessionId, String content) {
        append(sessionId, new ChatbotConversationMessage("user", content));
    }

    public void appendAssistantMessage(String sessionId, String content) {
        append(sessionId, new ChatbotConversationMessage("assistant", content));
    }

    private void append(String sessionId, ChatbotConversationMessage message) {
        conversations.compute(sessionId, (key, existing) -> {
            List<ChatbotConversationMessage> updated = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            updated.add(message);
            if (updated.size() > MAX_MESSAGES) {
                return new ArrayList<>(updated.subList(updated.size() - MAX_MESSAGES, updated.size()));
            }
            return updated;
        });
    }
}
