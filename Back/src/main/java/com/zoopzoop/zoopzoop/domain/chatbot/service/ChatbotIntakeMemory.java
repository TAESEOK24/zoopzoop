package com.zoopzoop.zoopzoop.domain.chatbot.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Component;

@Component
public class ChatbotIntakeMemory {

    private final Map<String, ChatbotIntakeProfile> profiles = new ConcurrentHashMap<>();

    public ChatbotIntakeProfile getProfile(String sessionId) {
        return profiles.computeIfAbsent(sessionId, key -> new ChatbotIntakeProfile());
    }

    public ChatbotIntakeProfile updateProfile(String sessionId, UnaryOperator<ChatbotIntakeProfile> updater) {
        return profiles.compute(sessionId, (key, existing) -> updater.apply(existing == null ? new ChatbotIntakeProfile() : existing));
    }

    public void clear(String sessionId) {
        profiles.remove(sessionId);
    }

    public static class ChatbotIntakeProfile {

        private String concernMessage;
        private Integer age;
        private String ageGroup;
        private String householdType;
        private String employmentStatus;
        private String housingStatus;
        private String awaitingField;

        public String concernMessage() {
            return concernMessage;
        }

        public void concernMessage(String concernMessage) {
            this.concernMessage = concernMessage;
        }

        public String ageGroup() {
            return ageGroup;
        }

        public Integer age() {
            return age;
        }

        public void age(Integer age) {
            this.age = age;
        }

        public void ageGroup(String ageGroup) {
            this.ageGroup = ageGroup;
        }

        public String householdType() {
            return householdType;
        }

        public void householdType(String householdType) {
            this.householdType = householdType;
        }

        public String employmentStatus() {
            return employmentStatus;
        }

        public void employmentStatus(String employmentStatus) {
            this.employmentStatus = employmentStatus;
        }

        public String housingStatus() {
            return housingStatus;
        }

        public void housingStatus(String housingStatus) {
            this.housingStatus = housingStatus;
        }

        public String awaitingField() {
            return awaitingField;
        }

        public void awaitingField(String awaitingField) {
            this.awaitingField = awaitingField;
        }

        public int completedFieldCount() {
            int count = 0;
            if (ageGroup != null) {
                count++;
            }
            if (householdType != null) {
                count++;
            }
            if (employmentStatus != null) {
                count++;
            }
            if (housingStatus != null) {
                count++;
            }
            return count;
        }

        public boolean isAwaitingClarification() {
            return awaitingField != null;
        }
    }
}
