package com.zoopzoop.zoopzoop.domain.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

public class UserUpdateRequest {
    @Data
    @NoArgsConstructor
    public static class Profile {
        private String name;
        private String email;          // 🚀 추가
        private String profileImageUrl; // 🚀 추가
    }

    @Data
    @NoArgsConstructor
    public static class Password {
        private String currentPassword;
        private String newPassword;
    }
}