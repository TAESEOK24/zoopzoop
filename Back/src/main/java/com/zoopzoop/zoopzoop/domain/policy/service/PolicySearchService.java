package com.zoopzoop.zoopzoop.domain.policy.service;

import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyDetailResultDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyDetail;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyDetailRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyListRepository;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicySearchService {

    private static final int DEFAULT_SIZE = 5;
    private static final int MAX_SIZE = 10;
    private static final List<String> STOP_WORDS = List.of(
            "정책", "지원", "알려줘", "알려주세요", "추천", "찾아줘", "찾아주세요",
            "뭐야", "뭐있어", "있어", "문의", "신청", "가능", "대상", "관련"
    );

    private final PolicyListRepository policyListRepository;
    private final PolicyDetailRepository policyDetailRepository;

    public List<PolicySearchResultDto> searchPolicies(String keyword, Integer size) {
        return searchPolicies(keyword, size, null);
    }

    public List<PolicySearchResultDto> searchPolicies(String keyword, Integer size, Integer age) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int normalizedSize = normalizeSize(size);

        List<PolicyList> directMatches = searchByKeyword(
                normalizedKeyword,
                age,
                PageRequest.of(0, normalizedSize)
        );

        if (!directMatches.isEmpty()) {
            return directMatches.stream()
                    .map(this::toSearchResultDto)
                    .toList();
        }

        return searchByKeywordTokens(normalizedKeyword, normalizedSize, age).stream()
                .map(this::toSearchResultDto)
                .toList();
    }

    public PolicyDetailResultDto getPolicyDetail(String serviceId) {
        String normalizedServiceId = normalizeServiceId(serviceId);

        PolicyList policyList = policyListRepository.findById(normalizedServiceId)
                .orElseThrow(() -> new AppException(404, "정책을 찾을 수 없습니다."));

        PolicyDetail policyDetail = policyDetailRepository.findById(normalizedServiceId).orElse(null);

        return toDetailResultDto(policyList, policyDetail);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new AppException(400, "검색어를 입력해주세요.");
        }

        return keyword.trim();
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }

        if (size < 1) {
            throw new AppException(400, "size는 1 이상이어야 합니다.");
        }

        return Math.min(size, MAX_SIZE);
    }

    private List<PolicyList> searchByKeywordTokens(String keyword, int size, Integer age) {
        List<String> tokens = extractSearchTokens(keyword);
        if (tokens.isEmpty()) {
            return List.of();
        }

        Map<String, PolicyList> deduplicated = new LinkedHashMap<>();
        for (String token : tokens) {
            List<PolicyList> matches = searchByKeyword(token, age, PageRequest.of(0, size));
            for (PolicyList policy : matches) {
                deduplicated.putIfAbsent(policy.getServiceId(), policy);
                if (deduplicated.size() >= size) {
                    return deduplicated.values().stream().toList();
                }
            }
        }

        return deduplicated.values().stream().toList();
    }

    private List<PolicyList> searchByKeyword(String keyword, Integer age, PageRequest pageRequest) {
        if (age == null) {
            return policyListRepository.searchByKeyword(keyword, pageRequest);
        }

        return policyListRepository.searchByKeywordAndAge(keyword, age, pageRequest);
    }

    private List<String> extractSearchTokens(String keyword) {
        return Arrays.stream(keyword.toLowerCase(Locale.ROOT).split("\\s+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .filter(token -> token.length() >= 2)
                .filter(token -> STOP_WORDS.stream().noneMatch(token::contains))
                .distinct()
                .toList();
    }

    private String normalizeServiceId(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new AppException(400, "serviceId를 입력해주세요.");
        }

        return serviceId.trim();
    }

    private PolicySearchResultDto toSearchResultDto(PolicyList policy) {
        return new PolicySearchResultDto(
                policy.getServiceId(),
                policy.getServiceName(),
                policy.getPurposeSummary(),
                policy.getTarget(),
                policy.getSupportContent(),
                policy.getApplicationMethod(),
                policy.getApplicationDeadline(),
                policy.getDetailUrl(),
                policy.getOrgName(),
                policy.getDepartmentName()
        );
    }

    private PolicyDetailResultDto toDetailResultDto(PolicyList policyList, PolicyDetail policyDetail) {
        return new PolicyDetailResultDto(
                policyList.getServiceId(),
                policyList.getServiceName(),
                policyList.getPurposeSummary(),
                policyList.getTarget(),
                policyList.getSelectionCriteria(),
                policyList.getSupportContent(),
                policyList.getApplicationMethod(),
                policyList.getApplicationDeadline(),
                policyList.getDetailUrl(),
                policyList.getOrgName(),
                policyList.getDepartmentName(),
                policyList.getContactNumber(),
                policyDetail != null ? policyDetail.getPurpose() : null,
                policyDetail != null ? policyDetail.getRequiredDocuments() : null,
                policyDetail != null ? policyDetail.getReceivingOrgName() : null,
                policyDetail != null ? policyDetail.getContactInfo() : null,
                policyDetail != null ? policyDetail.getOnlineUrl() : null,
                policyDetail != null ? policyDetail.getAdminRule() : null,
                policyDetail != null ? policyDetail.getLocalRule() : null,
                policyDetail != null ? policyDetail.getLaw() : null,
                policyDetail != null ? policyDetail.getOfficialRequiredDocs() : null,
                policyDetail != null ? policyDetail.getUserRequiredDocs() : null
        );
    }
}
