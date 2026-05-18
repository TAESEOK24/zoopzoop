package com.zoopzoop.zoopzoop.domain.admin.dto;

import com.zoopzoop.zoopzoop.domain.user.entity.User;
import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String name,
        String role,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}