package com.zoopzoop.zoopzoop.domain.policy.service;

import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyConditionTagsResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyDetailResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyPageResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchCriteria;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySummaryResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyTypeCountResponse;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyConditions;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyDetail;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyConditionsRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyDetailRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyListRepository;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyQueryService {

    private final PolicyListRepository policyListRepository;
    private final PolicyDetailRepository policyDetailRepository;
    private final PolicyConditionsRepository policyConditionsRepository;

    public PolicyQueryService(
            PolicyListRepository policyListRepository,
            PolicyDetailRepository policyDetailRepository,
            PolicyConditionsRepository policyConditionsRepository
    ) {
        this.policyListRepository = policyListRepository;
        this.policyDetailRepository = policyDetailRepository;
        this.policyConditionsRepository = policyConditionsRepository;
    }

    @Transactional(readOnly = true)
    public PolicyPageResponse getPolicies(String query, String type, Integer age, String region, String special, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("viewCount"),
                Sort.Order.asc("serviceName")
        ));

        PolicySearchCriteria criteria = new PolicySearchCriteria(
                normalize(query),
                normalize(type),
                age,
                normalize(region),
                parseSpecialCodes(special)
        );

        Page<PolicyList> resultPage = policyListRepository.searchPolicies(criteria, pageable, sort);

        List<PolicySummaryResponse> items = resultPage.getContent().stream()
                .map(PolicySummaryResponse::from)
                .toList();

        return new PolicyPageResponse(
                items,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resultPage.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public List<PolicyTypeCountResponse> getPolicyTypeCounts(String query, Integer age, String region, String special) {
        Map<String, Long> counts = new HashMap<>();
        PolicySearchCriteria criteria = new PolicySearchCriteria(
                normalize(query),
                null,
                age,
                normalize(region),
                parseSpecialCodes(special)
        );

        List<String> rawTypes = policyListRepository.findServiceTypes(criteria);

        for (String rawType : rawTypes) {
            if (!hasText(rawType)) {
                continue;
            }

            for (String atomicType : rawType.split("\\|\\|")) {
                String normalizedType = atomicType.trim();
                if (!normalizedType.isEmpty()) {
                    counts.merge(normalizedType, 1L, Long::sum);
                }
            }
        }

        return counts.entrySet().stream()
                .map(entry -> new PolicyTypeCountResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(PolicyTypeCountResponse::count).reversed()
                        .thenComparing(PolicyTypeCountResponse::type))
                .toList();
    }

    @Transactional(readOnly = true)
    public PolicyDetailResponse getPolicyDetail(String serviceId) {
        PolicyList policy = policyListRepository.findById(serviceId)
                .orElseThrow(() -> new AppException(404, "Policy not found."));

        Optional<PolicyDetail> detail = policyDetailRepository.findById(serviceId);
        Optional<PolicyConditions> conditions = policyConditionsRepository.findById(serviceId);

        return new PolicyDetailResponse(
                policy.getServiceId(),
                policy.getServiceName(),
                policy.getServiceType(),
                policy.getPurposeSummary(),
                detail.map(PolicyDetail::getPurpose).orElse(null),
                policy.getTarget(),
                policy.getSelectionCriteria(),
                policy.getSupportContent(),
                policy.getApplicationMethod(),
                policy.getApplicationDeadline(),
                policy.getDetailUrl(),
                firstNonBlank(policy.getOrgName(), detail.map(PolicyDetail::getOrgName).orElse(null)),
                policy.getDepartmentName(),
                policy.getContactNumber(),
                detail.map(PolicyDetail::getContactInfo).orElse(null),
                policy.getReceivingOrg(),
                detail.map(PolicyDetail::getReceivingOrgName).orElse(null),
                detail.map(PolicyDetail::getRequiredDocuments).orElse(null),
                detail.map(PolicyDetail::getOfficialRequiredDocs).orElse(null),
                detail.map(PolicyDetail::getUserRequiredDocs).orElse(null),
                detail.map(PolicyDetail::getOnlineUrl).orElse(null),
                detail.map(PolicyDetail::getAdminRule).orElse(null),
                detail.map(PolicyDetail::getLocalRule).orElse(null),
                detail.map(PolicyDetail::getLaw).orElse(null),
                policy.getViewCount(),
                conditions.map(this::toConditionTags).orElse(emptyConditionTags())
        );
    }

    private PolicyConditionTagsResponse toConditionTags(PolicyConditions conditions) {
        List<String> gender = new ArrayList<>();
        List<String> income = new ArrayList<>();
        List<String> lifeStage = new ArrayList<>();
        List<String> household = new ArrayList<>();
        List<String> business = new ArrayList<>();
        List<String> organization = new ArrayList<>();
        List<String> specialStatus = new ArrayList<>();

        addIfEnabled(gender, conditions.getJa0101(), "남성");
        addIfEnabled(gender, conditions.getJa0102(), "여성");
        addIfEnabled(income, conditions.getJa0201(), "중위소득 0~50%");
        addIfEnabled(income, conditions.getJa0202(), "중위소득 51~75%");
        addIfEnabled(income, conditions.getJa0203(), "중위소득 76~100%");
        addIfEnabled(income, conditions.getJa0204(), "중위소득 101~200%");
        addIfEnabled(income, conditions.getJa0205(), "중위소득 200% 초과");
        addIfEnabled(lifeStage, conditions.getJa0301(), "예비부모/난임");
        addIfEnabled(lifeStage, conditions.getJa0302(), "임산부");
        addIfEnabled(lifeStage, conditions.getJa0303(), "출산/입양");
        addIfEnabled(lifeStage, conditions.getJa0313(), "농업인");
        addIfEnabled(lifeStage, conditions.getJa0314(), "어업인");
        addIfEnabled(lifeStage, conditions.getJa0315(), "축산업인");
        addIfEnabled(lifeStage, conditions.getJa0316(), "임업인");
        addIfEnabled(lifeStage, conditions.getJa0317(), "초등학생");
        addIfEnabled(lifeStage, conditions.getJa0318(), "중학생");
        addIfEnabled(lifeStage, conditions.getJa0319(), "고등학생");
        addIfEnabled(lifeStage, conditions.getJa0320(), "대학생/대학원생");
        addIfEnabled(lifeStage, conditions.getJa0326(), "근로자/직장인");
        addIfEnabled(lifeStage, conditions.getJa0327(), "구직자/실업자");
        addIfEnabled(lifeStage, conditions.getJa0328(), "장애인");
        addIfEnabled(lifeStage, conditions.getJa0329(), "국가보훈대상자");
        addIfEnabled(lifeStage, conditions.getJa0330(), "질병/질환자");
        addIfEnabled(lifeStage, conditions.getJa0322(), "해당사항 없음");
        addIfEnabled(household, conditions.getJa0401(), "다문화가족");
        addIfEnabled(household, conditions.getJa0402(), "북한이탈주민");
        addIfEnabled(household, conditions.getJa0403(), "한부모가정/조손가정");
        addIfEnabled(household, conditions.getJa0404(), "1인가구");
        addIfEnabled(household, conditions.getJa0411(), "다자녀가구");
        addIfEnabled(household, conditions.getJa0412(), "무주택세대");
        addIfEnabled(household, conditions.getJa0413(), "신규전입");
        addIfEnabled(household, conditions.getJa0414(), "확대가족");
        addIfEnabled(household, conditions.getJa0410(), "해당사항 없음");
        addIfEnabled(business, conditions.getJa1101(), "예비창업자");
        addIfEnabled(business, conditions.getJa1102(), "영업중");
        addIfEnabled(business, conditions.getJa1103(), "생계곤란/폐업예정자");
        addIfEnabled(business, conditions.getJa1201(), "음식점업");
        addIfEnabled(business, conditions.getJa1202(), "제조업");
        addIfEnabled(business, conditions.getJa1299(), "기타업종");
        addIfEnabled(organization, conditions.getJa2101(), "중소기업");
        addIfEnabled(organization, conditions.getJa2102(), "사회복지시설");
        addIfEnabled(organization, conditions.getJa2103(), "기관/단체");
        addIfEnabled(organization, conditions.getJa2201(), "제조업");
        addIfEnabled(organization, conditions.getJa2202(), "농업,임업 및 어업");
        addIfEnabled(organization, conditions.getJa2203(), "정보통신업");
        addIfEnabled(organization, conditions.getJa2299(), "기타업종");
        addIfEnabled(specialStatus, conditions.getJa0328(), "장애인");
        addIfEnabled(specialStatus, conditions.getJa0329(), "국가보훈대상자");
        addIfEnabled(specialStatus, conditions.getJa0330(), "질병/질환자");

        income = normalizeRestrictionGroup(income, 5, false);
        lifeStage = normalizeRestrictionGroup(lifeStage, 16, isEnabled(conditions.getJa0322()));
        household = normalizeRestrictionGroup(household, 8, isEnabled(conditions.getJa0410()));
        specialStatus = normalizeRestrictionGroup(specialStatus, 3, false);

        return new PolicyConditionTagsResponse(
                gender,
                conditions.getJa0110(),
                conditions.getJa0111(),
                income,
                lifeStage,
                household,
                business,
                organization,
                specialStatus
        );
    }

    private PolicyConditionTagsResponse emptyConditionTags() {
        return new PolicyConditionTagsResponse(
                List.of(),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private void addIfEnabled(List<String> target, String value, String label) {
        if (isEnabled(value)) {
            target.add(label);
        }
    }

    private List<String> normalizeRestrictionGroup(List<String> values, int totalOptions, boolean hasNoRestrictionFlag) {
        if (values.isEmpty()) {
            return values;
        }
        if (hasNoRestrictionFlag || values.size() >= totalOptions) {
            return List.of("제한 없음");
        }
        return values;
    }

    private boolean isEnabled(String value) {
        if (!hasText(value)) {
            return false;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return !normalized.equals("N") && !normalized.equals("0") && !normalized.equals("FALSE");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalize(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private List<String> parseSpecialCodes(String value) {
        if (!hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(this::normalize)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private String firstNonBlank(String first, String second) {
        if (hasText(first)) {
            return first;
        }
        return hasText(second) ? second : null;
    }
}
