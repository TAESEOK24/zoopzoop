package com.zoopzoop.zoopzoop.domain.chatbot.service;

import com.zoopzoop.zoopzoop.domain.chatbot.dto.ChatbotResponseType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ChatbotIntentClassifier {

    private static final List<String> SAFETY_KEYWORDS = List.of(
            "죽고 싶", "자살", "극단적", "응급", "119", "숨이 안", "숨이 차", "호흡이 안",
            "너무 아파", "심하게 아파", "쓰러질", "의식이", "피가", "가슴이 너무 아파"
    );

    private static final List<String> POLICY_KEYWORDS = List.of(
            "정책", "복지", "지원", "지원금", "수당", "급여", "주거", "월세", "전세", "청년",
            "구직", "취업", "실업", "돌봄", "출산", "육아", "생계", "긴급복지", "대출", "신청"
    );

    private static final List<String> POLICY_ASK_KEYWORDS = List.of(
            "알려", "추천", "찾아", "뭐가 있", "있을까", "신청", "받을 수", "대상", "조건", "문의"
    );

    private static final List<String> HARDSHIP_KEYWORDS = List.of(
            "가난", "생활이 힘들", "너무 힘들", "돈이 없", "월세 내기 힘들", "생활비", "주거비",
            "생계가 어렵", "실직", "구직 중", "일자리가 없", "먹고살기 힘들", "버티기 힘들"
    );

    private static final List<String> SMALLTALK_KEYWORDS = List.of(
            "안녕", "하이", "반가", "고마", "감사", "심심해", "뭐해", "잘 지내"
    );

    public ChatbotResponseType classify(String rawMessage, boolean awaitingClarification) {
        String message = normalize(rawMessage);

        if (containsAny(message, SAFETY_KEYWORDS)) {
            return ChatbotResponseType.SAFETY;
        }

        if (containsAny(message, HARDSHIP_KEYWORDS)) {
            return ChatbotResponseType.CLARIFICATION_NEEDED;
        }

        if (containsAny(message, POLICY_KEYWORDS) && containsAny(message, POLICY_ASK_KEYWORDS)) {
            return ChatbotResponseType.POLICY_SEARCH;
        }

        if (awaitingClarification && isLikelyProfileAnswer(message)) {
            return ChatbotResponseType.CLARIFICATION_NEEDED;
        }

        if (containsAny(message, SMALLTALK_KEYWORDS)) {
            return ChatbotResponseType.SMALLTALK;
        }

        return ChatbotResponseType.OFF_TOPIC;
    }

    private boolean isLikelyProfileAnswer(String message) {
        return message.length() <= 20;
    }

    private boolean containsAny(String message, List<String> keywords) {
        return keywords.stream().anyMatch(message::contains);
    }

    private String normalize(String rawMessage) {
        return rawMessage == null ? "" : rawMessage.trim().toLowerCase();
    }
}
