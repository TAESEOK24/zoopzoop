package com.zoopzoop.zoopzoop.domain.user.dto;

import com.zoopzoop.zoopzoop.domain.user.entity.User;
import java.time.LocalDateTime;

public record UserSummary(
        Long id,
        String email,
        String name,
        String profileImageUrl,
        Integer age,
        String gender,
        String region,
        String district,
        String maritalStatus,
        String employmentStatus,
        Integer householdSize,
        Integer income,
        Integer incomeBracket, // 🚀 소득 구간 반환
        String role,
        LocalDateTime createdAt
) {
    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getAge(),
                user.getGender(),
                user.getRegion(),
                user.getDistrict(),
                user.getMaritalStatus(),
                user.getEmploymentStatus(),
                user.getHouseholdSize(),
                user.getIncome(),
                user.getIncomeBracket(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getCreatedAt()
        );
    }
}