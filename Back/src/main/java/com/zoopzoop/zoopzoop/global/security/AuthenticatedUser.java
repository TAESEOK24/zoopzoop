package com.zoopzoop.zoopzoop.global.security;

public record AuthenticatedUser(
        Long id,
        String email,
        String name,
        String role
) {
}
