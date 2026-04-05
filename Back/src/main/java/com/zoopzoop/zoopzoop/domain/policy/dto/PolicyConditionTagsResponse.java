package com.zoopzoop.zoopzoop.domain.policy.dto;

import java.util.List;

public record PolicyConditionTagsResponse(
        List<String> gender,
        Integer minAge,
        Integer maxAge,
        List<String> income,
        List<String> lifeStage,
        List<String> household,
        List<String> business,
        List<String> organization,
        List<String> specialStatus
) {
}
