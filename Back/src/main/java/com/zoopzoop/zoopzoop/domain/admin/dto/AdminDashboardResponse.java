package com.zoopzoop.zoopzoop.domain.admin.dto;

public record AdminDashboardResponse(
        long totalUsers,
        long todayPosts,
        long totalPolicies
) {
}