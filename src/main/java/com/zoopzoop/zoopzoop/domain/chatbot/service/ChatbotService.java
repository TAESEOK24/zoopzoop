package com.zoopzoop.zoopzoop.domain.chatbot.service;

import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("chatbot", "chatbot module ready");
    }
}
