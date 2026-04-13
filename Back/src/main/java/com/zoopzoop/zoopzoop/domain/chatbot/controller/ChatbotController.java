package com.zoopzoop.zoopzoop.domain.chatbot.controller;

import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAskRequest;
import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotAskResponse;
import com.zoopzoop.zoopzoop.domain.chatbot.service.ChatbotService;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @GetMapping("/health")
    public ApiResponse<HealthCheckDto> health() {
        return ApiResponse.ok(chatbotService.getStatus());
    }

    @PostMapping("/ask")
    public ApiResponse<ChatbotAskResponse> ask(@Valid @RequestBody ChatbotAskRequest request) {
        return ApiResponse.ok(chatbotService.ask(request.sessionId(), request.message()));
    }
}
