package com.zoopzoop.zoopzoop.domain.policy.dto;

import java.util.List;

public record PolicyPageResponse(
        List<PolicySummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
