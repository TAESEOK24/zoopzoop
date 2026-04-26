package com.zoopzoop.zoopzoop.domain.searchlog.service;

import com.zoopzoop.zoopzoop.domain.searchlog.entity.SearchLog;
import com.zoopzoop.zoopzoop.domain.searchlog.repository.SearchLogRepository;
import com.zoopzoop.zoopzoop.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchLogService {

    private static final int MAX_KEYWORD_LENGTH = 255;
    private static final int MAX_SERVICE_ID_LENGTH = 100;
    private static final String ACTION_SEARCH = "SEARCH";
    private static final String ACTION_VIEW = "VIEW";

    private final SearchLogRepository searchLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSearch(AuthenticatedUser user, String keyword) {
        if (user == null) {
            return;
        }

        String normalizedKeyword = normalize(keyword, MAX_KEYWORD_LENGTH);
        if (normalizedKeyword == null) {
            return;
        }
        save(user, normalizedKeyword, null, ACTION_SEARCH);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPolicyView(AuthenticatedUser user, String serviceId) {
        if (user == null) {
            return;
        }

        String normalizedServiceId = normalize(serviceId, MAX_SERVICE_ID_LENGTH);
        if (normalizedServiceId == null) {
            return;
        }

        save(user, null, normalizedServiceId, ACTION_VIEW);
    }

    private void save(AuthenticatedUser user, String keyword, String serviceId, String actionType) {
        searchLogRepository.save(SearchLog.builder()
                .userId(Math.toIntExact(user.id()))
                .keyword(keyword)
                .serviceId(serviceId)
                .actionType(actionType)
                .build());
    }

    private String normalize(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
