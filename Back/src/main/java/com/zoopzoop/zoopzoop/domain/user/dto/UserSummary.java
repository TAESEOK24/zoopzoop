package com.zoopzoop.zoopzoop.domain.user.dto;

import com.zoopzoop.zoopzoop.domain.user.entity.Role;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import java.time.LocalDateTime;

public record UserSummary(
        Long id,
        String email,
        String name,
        Role role,
        String profileImageUrl, // 🚀 추가
        LocalDateTime createdAt // 🚀 추가
) {
    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getProfileImageUrl(),
                user.getCreatedAt()
        );
    }
}