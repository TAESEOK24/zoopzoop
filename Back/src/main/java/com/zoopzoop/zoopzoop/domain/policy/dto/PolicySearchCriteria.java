package com.zoopzoop.zoopzoop.domain.policy.dto;

import java.util.List;

public record PolicySearchCriteria(
        String query,
        String type,
        Integer age,
        String region,
        List<String> specialCodes
) {
}
