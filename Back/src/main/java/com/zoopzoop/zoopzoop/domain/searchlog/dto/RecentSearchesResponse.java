package com.zoopzoop.zoopzoop.domain.searchlog.dto;

import java.util.List;

public record RecentSearchesResponse(
        List<String> keywords
) {
}
