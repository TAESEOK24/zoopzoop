package com.zoopzoop.zoopzoop.domain.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

public class UserUpdateRequest {

    @Data
    @NoArgsConstructor
    public static class Profile {
        private String name;
        private String email;
        private String profileImageUrl;
        private Integer age;
        private String gender;
        private String region;
        private String district;
        private String maritalStatus;
        private String employmentStatus;
        private Integer householdSize;
        private Integer income;
    }

    @Data
    @NoArgsConstructor
    public static class Password {
        private String currentPassword;
        private String newPassword;
    }
}