package com.zoopzoop.zoopzoop.domain.user.dto;

import com.zoopzoop.zoopzoop.domain.user.entity.User;

public record UserSummary(
        Long id,
        String email,
        String name,
        String role
) {
    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );
    }
}
