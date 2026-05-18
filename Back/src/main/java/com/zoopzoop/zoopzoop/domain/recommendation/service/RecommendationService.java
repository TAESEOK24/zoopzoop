package com.zoopzoop.zoopzoop.domain.recommendation.service;

import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyConditions;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyConditionsRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyListRepository;
import com.zoopzoop.zoopzoop.domain.recommendation.dto.ProfileRecommendationResponse;
import com.zoopzoop.zoopzoop.domain.recommendation.dto.RecommendationItemResponse;
import com.zoopzoop.zoopzoop.domain.recommendation.dto.RecommendationResponse;
import com.zoopzoop.zoopzoop.domain.searchlog.entity.SearchLog;
import com.zoopzoop.zoopzoop.domain.searchlog.repository.SearchLogRepository;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import com.zoopzoop.zoopzoop.global.security.AuthenticatedUser;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final String ACTION_SEARCH = "SEARCH";
    private static final String ACTION_VIEW = "VIEW";
    private static final int DEFAULT_SIZE = 6;
    private static final int MAX_SIZE = 12;
    private static final int KEYWORD_CANDIDATE_LIMIT = 10;
    private static final int VIEW_CANDIDATE_LIMIT = 8;
    private static final int PROFILE_CANDIDATE_LIMIT = 10;
    private static final double KEYWORD_WEIGHT = 0.40;
    private static final double VIEW_WEIGHT = 0.30;
    private static final double RECENCY_WEIGHT = 0.15;
    private static final double FREQUENCY_WEIGHT = 0.10;
    private static final double POPULARITY_WEIGHT = 0.05;

    private final SearchLogRepository searchLogRepository;
    private final PolicyListRepository policyListRepository;
    private final PolicyConditionsRepository policyConditionsRepository;
    private final UserRepository userRepository;

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("recommendation", "recommendation module ready");
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getPersonalizedRecommendations(AuthenticatedUser user, Integer size) {
        if (user == null) {
            throw new AppException(401, "Authentication is required.");
        }

        int normalizedSize = normalizeSize(size);
        List<SearchLog> searchLogs = searchLogRepository.findTop30ByUserIdAndActionTypeOrderByActionTimeDesc(
                Math.toIntExact(user.id()),
                ACTION_SEARCH
        );
        List<SearchLog> viewLogs = searchLogRepository.findTop30ByUserIdAndActionTypeOrderByActionTimeDesc(
                Math.toIntExact(user.id()),
                ACTION_VIEW
        );

        Set<String> viewedPolicyIds = viewLogs.stream()
                .map(SearchLog::getServiceId)
                .filter(this::hasText)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        if (searchLogs.isEmpty() && viewedPolicyIds.isEmpty()) {
            return new RecommendationResponse(buildPopularRecommendations(normalizedSize, viewedPolicyIds));
        }

        Map<String, CandidateScore> candidates = new HashMap<>();
        Map<String, Integer> keywordCounts = new HashMap<>();
        List<String> matchedKeywords = collectSearchKeywords(searchLogs, keywordCounts);
        scoreKeywordCandidates(candidates, matchedKeywords, keywordCounts, searchLogs);
        scoreViewedPolicyCandidates(candidates, new ArrayList<>(viewedPolicyIds), viewLogs);

        if (candidates.isEmpty()) {
            return new RecommendationResponse(buildPopularRecommendations(normalizedSize, viewedPolicyIds));
        }

        double maxViewCount = candidates.values().stream()
                .map(CandidateScore::policy)
                .map(PolicyList::getViewCount)
                .filter(viewCount -> viewCount != null && viewCount > 0)
                .mapToDouble(Integer::doubleValue)
                .max()
                .orElse(1.0);

        List<RecommendationItemResponse> items = candidates.values().stream()
                .filter(candidate -> !viewedPolicyIds.contains(candidate.policy().getServiceId()))
                .peek(candidate -> candidate.finalizeScore(maxViewCount))
                .sorted(Comparator.comparingDouble(CandidateScore::totalScore).reversed()
                        .thenComparing(candidate -> safeText(candidate.policy().getServiceName())))
                .limit(normalizedSize)
                .map(candidate -> RecommendationItemResponse.of(
                        candidate.policy(),
                        candidate.buildReason(),
                        roundScore(candidate.totalScore())
                ))
                .toList();

        if (items.isEmpty()) {
            return new RecommendationResponse(buildPopularRecommendations(normalizedSize, viewedPolicyIds));
        }

        return new RecommendationResponse(items);
    }

    @Transactional(readOnly = true)
    public ProfileRecommendationResponse getProfileBasedRecommendations(AuthenticatedUser user, Integer size) {
        if (user == null) {
            throw new AppException(401, "Authentication is required.");
        }

        User profile = userRepository.findById(user.id())
                .orElseThrow(() -> new AppException(404, "User not found."));
        if (!hasUsableProfile(profile)) {
            return ProfileRecommendationResponse.notReady();
        }

        int normalizedSize = normalizeSize(size);
        Map<String, ProfileCandidateScore> candidates = new HashMap<>();
        Map<String, PolicyConditions> conditionsByPolicyId = new HashMap<>();

        for (String keyword : buildProfileKeywords(profile)) {
            List<PolicyList> policies = policyListRepository.searchByKeyword(
                    keyword,
                    PageRequest.of(0, PROFILE_CANDIDATE_LIMIT)
            );
            for (PolicyList policy : policies) {
                scoreProfileCandidate(candidates, conditionsByPolicyId, policy, profile);
            }
        }

        List<PolicyList> latestPolicies = policyListRepository.findAll(PageRequest.of(
                0,
                PROFILE_CANDIDATE_LIMIT,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("viewCount"))
        )).stream().toList();
        for (PolicyList policy : latestPolicies) {
            scoreProfileCandidate(candidates, conditionsByPolicyId, policy, profile);
        }

        List<RecommendationItemResponse> items = candidates.values().stream()
                .filter(candidate -> candidate.totalScore() > 0)
                .sorted(Comparator.comparingDouble(ProfileCandidateScore::totalScore).reversed()
                        .thenComparing(candidate -> safeNumber(candidate.policy().getViewCount()), Comparator.reverseOrder())
                        .thenComparing(candidate -> safeText(candidate.policy().getServiceName())))
                .limit(normalizedSize)
                .map(candidate -> RecommendationItemResponse.of(
                        candidate.policy(),
                        candidate.buildReason(),
                        roundScore(candidate.totalScore() / 100.0)
                ))
                .toList();

        return ProfileRecommendationResponse.ready(items);
    }

    private void scoreKeywordCandidates(
            Map<String, CandidateScore> candidates,
            List<String> keywords,
            Map<String, Integer> keywordCounts,
            List<SearchLog> searchLogs
    ) {
        Map<String, LocalDateTime> latestByKeyword = new HashMap<>();
        for (SearchLog log : searchLogs) {
            for (String token : extractTokens(log.getKeyword())) {
                latestByKeyword.merge(token, log.getActionTime(), this::latest);
            }
        }

        for (String keyword : keywords) {
            List<PolicyList> policies = policyListRepository.searchByKeyword(
                    keyword,
                    PageRequest.of(0, KEYWORD_CANDIDATE_LIMIT)
            );

            for (PolicyList policy : policies) {
                CandidateScore candidate = candidates.computeIfAbsent(
                        policy.getServiceId(),
                        ignored -> new CandidateScore(policy)
                );
                double textMatchScore = calculateKeywordTextMatch(policy, keyword);
                if (textMatchScore <= 0) {
                    continue;
                }

                int count = keywordCounts.getOrDefault(keyword, 1);
                candidate.keywordScore += textMatchScore;
                candidate.recencyScore += recencyWeight(latestByKeyword.get(keyword));
                candidate.frequencyScore += frequencyWeight(count);
                candidate.keywordMatches.add(keyword);
            }
        }
    }

    private void scoreViewedPolicyCandidates(
            Map<String, CandidateScore> candidates,
            List<String> viewedPolicyIds,
            List<SearchLog> viewLogs
    ) {
        Map<String, SearchLog> latestViewLogByPolicyId = new HashMap<>();
        for (SearchLog log : viewLogs) {
            if (hasText(log.getServiceId())) {
                latestViewLogByPolicyId.putIfAbsent(log.getServiceId(), log);
            }
        }

        for (String serviceId : viewedPolicyIds) {
            PolicyList viewedPolicy = policyListRepository.findById(serviceId).orElse(null);
            if (viewedPolicy == null) {
                continue;
            }

            SearchLog viewLog = latestViewLogByPolicyId.get(serviceId);
            LocalDateTime actionTime = viewLog != null ? viewLog.getActionTime() : null;

            if (hasText(viewedPolicy.getServiceType())) {
                List<PolicyList> sameTypePolicies = policyListRepository.findByServiceTypeContainingIgnoreCase(
                        viewedPolicy.getServiceType(),
                        PageRequest.of(0, VIEW_CANDIDATE_LIMIT)
                );
                scoreSimilarityGroup(candidates, viewedPolicy, sameTypePolicies, actionTime);
            }

            String organizationKey = firstNonBlank(viewedPolicy.getOrgName(), viewedPolicy.getDepartmentName());
            if (hasText(organizationKey)) {
                List<PolicyList> sameOrgPolicies = policyListRepository.searchByOrganization(
                        organizationKey,
                        PageRequest.of(0, VIEW_CANDIDATE_LIMIT)
                );
                scoreSimilarityGroup(candidates, viewedPolicy, sameOrgPolicies, actionTime);
            }
        }
    }

    private void scoreSimilarityGroup(
            Map<String, CandidateScore> candidates,
            PolicyList viewedPolicy,
            List<PolicyList> similarPolicies,
            LocalDateTime actionTime
    ) {
        for (PolicyList policy : similarPolicies) {
            CandidateScore candidate = candidates.computeIfAbsent(
                    policy.getServiceId(),
                    ignored -> new CandidateScore(policy)
            );
            double similarity = calculateViewSimilarity(viewedPolicy, policy);
            if (similarity <= 0) {
                continue;
            }

            candidate.viewScore += similarity;
            candidate.recencyScore += recencyWeight(actionTime);
            candidate.frequencyScore += frequencyWeight(1);
            candidate.similarServiceTypes.add(safeText(viewedPolicy.getServiceType()));
        }
    }

    private List<RecommendationItemResponse> buildPopularRecommendations(int size, Set<String> excludedPolicyIds) {
        return policyListRepository.findAll(PageRequest.of(
                        0,
                        size + excludedPolicyIds.size(),
                        Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("createdAt"))
                )).stream()
                .filter(policy -> !excludedPolicyIds.contains(policy.getServiceId()))
                .limit(size)
                .map(policy -> RecommendationItemResponse.of(policy, "Recent activity is limited, so popular policies are shown first.", 0.0))
                .toList();
    }

    private void scoreProfileCandidate(
            Map<String, ProfileCandidateScore> candidates,
            Map<String, PolicyConditions> conditionsByPolicyId,
            PolicyList policy,
            User profile
    ) {
        PolicyConditions conditions = conditionsByPolicyId.computeIfAbsent(
                policy.getServiceId(),
                serviceId -> policyConditionsRepository.findById(serviceId).orElse(null)
        );
        ProfileMatch match = calculateProfileMatch(policy, conditions, profile);
        if (match.score() <= 0) {
            return;
        }

        ProfileCandidateScore candidate = candidates.computeIfAbsent(
                policy.getServiceId(),
                ignored -> new ProfileCandidateScore(policy)
        );
        candidate.add(match);
    }

    private ProfileMatch calculateProfileMatch(PolicyList policy, PolicyConditions conditions, User profile) {
        double score = 0.0;
        Set<String> reasons = new LinkedHashSet<>();

        if (matchesAge(conditions, profile.getAge())) {
            score += 30.0;
            reasons.add("나이");
        }
        if (matchesIncomeBracket(conditions, profile.getIncomeBracket())) {
            score += 25.0;
            reasons.add("소득 구간");
        }
        double regionScore = calculateRegionScore(policy, profile);
        if (regionScore > 0) {
            score += regionScore;
            reasons.add("거주 지역");
        }
        double employmentScore = calculateEmploymentScore(policy, conditions, profile.getEmploymentStatus());
        if (employmentScore > 0) {
            score += employmentScore;
            reasons.add(employmentReason(profile.getEmploymentStatus()));
        }
        if (matchesGender(conditions, profile.getGender())) {
            score += 5.0;
            reasons.add("성별");
        }
        double householdScore = calculateHouseholdScore(policy, conditions, profile);
        if (householdScore > 0) {
            score += householdScore;
            reasons.add("가구 정보");
        }

        return new ProfileMatch(Math.min(score, 100.0), reasons);
    }

    private boolean hasUsableProfile(User profile) {
        return profile.getAge() != null
                || hasText(profile.getGender())
                || hasText(profile.getRegion())
                || hasText(profile.getDistrict())
                || hasText(profile.getMaritalStatus())
                || hasText(profile.getEmploymentStatus())
                || profile.getHouseholdSize() != null
                || profile.getIncomeBracket() != null;
    }

    private List<String> buildProfileKeywords(User profile) {
        Set<String> keywords = new LinkedHashSet<>();
        addIfPresent(keywords, profile.getRegion());
        addIfPresent(keywords, profile.getDistrict());
        addIfPresent(keywords, compactRegion(profile.getRegion()));
        addIfPresent(keywords, compactRegion(profile.getDistrict()));
        addIfPresent(keywords, employmentKeyword(profile.getEmploymentStatus()));

        if (profile.getAge() != null) {
            if (profile.getAge() <= 34) {
                keywords.add("청년");
            } else if (profile.getAge() >= 65) {
                keywords.add("노인");
                keywords.add("어르신");
            }
        }
        if (profile.getIncomeBracket() != null && profile.getIncomeBracket() <= 2) {
            keywords.add("저소득");
            keywords.add("중위소득");
        }
        if (profile.getHouseholdSize() != null && profile.getHouseholdSize() == 1) {
            keywords.add("1인 가구");
        }
        if (hasText(profile.getMaritalStatus()) && profile.getMaritalStatus().equalsIgnoreCase("SINGLE")) {
            keywords.add("미혼");
        }

        return new ArrayList<>(keywords);
    }

    private boolean matchesAge(PolicyConditions conditions, Integer age) {
        if (conditions == null || age == null) {
            return false;
        }
        Integer minAge = conditions.getJa0110();
        Integer maxAge = conditions.getJa0111();
        return (minAge == null || minAge <= age) && (maxAge == null || maxAge >= age);
    }

    private boolean matchesIncomeBracket(PolicyConditions conditions, Integer incomeBracket) {
        if (conditions == null || incomeBracket == null) {
            return false;
        }
        if (incomeBracket <= 2) {
            return isEnabled(conditions.getJa0201());
        }
        if (incomeBracket <= 4) {
            return isEnabled(conditions.getJa0202());
        }
        if (incomeBracket <= 5) {
            return isEnabled(conditions.getJa0203());
        }
        if (incomeBracket <= 8) {
            return isEnabled(conditions.getJa0204());
        }
        return isEnabled(conditions.getJa0205());
    }

    private double calculateRegionScore(PolicyList policy, User profile) {
        if (hasText(profile.getDistrict())) {
            String district = profile.getDistrict();
            if (matchesAnyText(district, policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(),
                    policy.getOrgName(), policy.getReceivingOrg(), policy.getDepartmentName())
                    || matchesAnyText(compactRegion(district), policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(),
                    policy.getOrgName(), policy.getReceivingOrg(), policy.getDepartmentName())) {
                return 20.0;
            }
        }
        if (hasText(profile.getRegion())) {
            String region = profile.getRegion();
            if (matchesAnyText(region, policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(),
                    policy.getOrgName(), policy.getReceivingOrg(), policy.getDepartmentName())
                    || matchesAnyText(compactRegion(region), policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(),
                    policy.getOrgName(), policy.getReceivingOrg(), policy.getDepartmentName())) {
                return 15.0;
            }
        }
        return 0.0;
    }

    private double calculateEmploymentScore(PolicyList policy, PolicyConditions conditions, String employmentStatus) {
        if (!hasText(employmentStatus)) {
            return 0.0;
        }
        String normalized = employmentStatus.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "EMPLOYED" -> isEnabled(conditions == null ? null : conditions.getJa0326())
                    || matchesAnyText("근로", policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(), policy.getSupportContent())
                    || matchesAnyText("직장", policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(), policy.getSupportContent())
                    ? 15.0 : 0.0;
            case "UNEMPLOYED" -> isEnabled(conditions == null ? null : conditions.getJa0327())
                    || matchesAnyText("구직", policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(), policy.getSupportContent())
                    || matchesAnyText("취업", policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(), policy.getSupportContent())
                    ? 15.0 : 0.0;
            case "STUDENT" -> isEnabled(conditions == null ? null : conditions.getJa0320())
                    || matchesAnyText("학생", policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(), policy.getSupportContent())
                    || matchesAnyText("대학생", policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(), policy.getSupportContent())
                    || matchesAnyText("학자금", policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(), policy.getSupportContent())
                    || matchesAnyText("장학", policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(), policy.getSupportContent())
                    ? 15.0 : 0.0;
            case "ENTREPRENEUR" -> isEnabled(conditions == null ? null : conditions.getJa0313())
                    || isEnabled(conditions == null ? null : conditions.getJa0314())
                    || matchesAnyText("창업", policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(), policy.getSupportContent())
                    ? 15.0 : 0.0;
            case "FREELANCER" -> matchesAnyText("프리랜서", policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(), policy.getSupportContent())
                    ? 15.0 : 0.0;
            default -> 0.0;
        };
    }

    private boolean matchesGender(PolicyConditions conditions, String gender) {
        if (conditions == null || !hasText(gender)) {
            return false;
        }
        String normalized = gender.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("MALE")) {
            return isEnabled(conditions.getJa0101());
        }
        if (normalized.equals("FEMALE")) {
            return isEnabled(conditions.getJa0102());
        }
        return false;
    }

    private double calculateHouseholdScore(PolicyList policy, PolicyConditions conditions, User profile) {
        if (profile.getHouseholdSize() != null && profile.getHouseholdSize() == 1
                && isEnabled(conditions == null ? null : conditions.getJa0404())) {
            return 5.0;
        }
        if (hasText(profile.getMaritalStatus()) && profile.getMaritalStatus().equalsIgnoreCase("SINGLE")
                && (matchesAnyText("미혼", policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(), policy.getSupportContent())
                || matchesAnyText("독립", policy.getServiceName(), policy.getPurposeSummary(), policy.getTarget(), policy.getSupportContent()))) {
            return 3.0;
        }
        return 0.0;
    }

    private boolean matchesAnyText(String needle, String... fields) {
        if (!hasText(needle)) {
            return false;
        }
        String normalizedNeedle = needle.trim().toLowerCase(Locale.ROOT);
        for (String field : fields) {
            if (hasText(field) && field.toLowerCase(Locale.ROOT).contains(normalizedNeedle)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEnabled(String value) {
        if (!hasText(value)) {
            return false;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return !normalized.equals("N") && !normalized.equals("0") && !normalized.equals("FALSE");
    }

    private void addIfPresent(Set<String> target, String value) {
        if (hasText(value)) {
            target.add(value.trim());
        }
    }

    private String compactRegion(String value) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        for (String suffix : List.of("특별자치도", "특별자치시", "특별시", "광역시")) {
            if (normalized.endsWith(suffix)) {
                return normalized.substring(0, normalized.length() - suffix.length());
            }
        }
        for (String suffix : List.of("도", "시", "군", "구", "동", "읍", "면", "리")) {
            if (normalized.endsWith(suffix)) {
                return normalized.substring(0, normalized.length() - suffix.length());
            }
        }
        return normalized;
    }

    private String employmentKeyword(String employmentStatus) {
        if (!hasText(employmentStatus)) {
            return null;
        }
        return switch (employmentStatus.trim().toUpperCase(Locale.ROOT)) {
            case "EMPLOYED" -> "근로";
            case "UNEMPLOYED" -> "구직";
            case "STUDENT" -> "대학생";
            case "ENTREPRENEUR" -> "창업";
            case "FREELANCER" -> "프리랜서";
            default -> null;
        };
    }

    private String employmentReason(String employmentStatus) {
        if (!hasText(employmentStatus)) {
            return "경제 활동 상태";
        }
        return switch (employmentStatus.trim().toUpperCase(Locale.ROOT)) {
            case "STUDENT" -> "학생 정보";
            case "UNEMPLOYED" -> "구직 상태";
            case "EMPLOYED" -> "근로 상태";
            case "ENTREPRENEUR" -> "창업 정보";
            case "FREELANCER" -> "프리랜서 정보";
            default -> "경제 활동 상태";
        };
    }

    private List<String> collectSearchKeywords(List<SearchLog> searchLogs, Map<String, Integer> keywordCounts) {
        Set<String> deduplicated = new LinkedHashSet<>();
        for (SearchLog log : searchLogs) {
            for (String token : extractTokens(log.getKeyword())) {
                deduplicated.add(token);
                keywordCounts.merge(token, 1, Integer::sum);
            }
        }
        return new ArrayList<>(deduplicated);
    }

    private List<String> extractTokens(String keyword) {
        if (!hasText(keyword)) {
            return List.of();
        }

        Set<String> tokens = new LinkedHashSet<>();
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() >= 2) {
            tokens.add(normalized);
        }

        for (String token : normalized.split("\\s+")) {
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }

        return new ArrayList<>(tokens);
    }

    private double calculateKeywordTextMatch(PolicyList policy, String keyword) {
        double score = 0.0;
        score += matchWeight(policy.getServiceName(), keyword, 1.0);
        score += matchWeight(policy.getPurposeSummary(), keyword, 0.8);
        score += matchWeight(policy.getTarget(), keyword, 0.7);
        score += matchWeight(policy.getSupportContent(), keyword, 0.6);
        score += matchWeight(policy.getApplicationMethod(), keyword, 0.4);
        score += matchWeight(policy.getDepartmentName(), keyword, 0.3);
        return Math.min(score, 1.0);
    }

    private double calculateViewSimilarity(PolicyList viewedPolicy, PolicyList candidate) {
        if (viewedPolicy.getServiceId().equals(candidate.getServiceId())) {
            return 0.0;
        }

        double score = 0.0;
        if (textEquals(viewedPolicy.getServiceType(), candidate.getServiceType())) {
            score += 0.4;
        }
        if (textEquals(viewedPolicy.getOrgName(), candidate.getOrgName())) {
            score += 0.2;
        }
        if (textEquals(viewedPolicy.getDepartmentName(), candidate.getDepartmentName())) {
            score += 0.1;
        }
        if (shareToken(viewedPolicy.getTarget(), candidate.getTarget())) {
            score += 0.2;
        }
        if (shareToken(viewedPolicy.getPurposeSummary(), candidate.getPurposeSummary())) {
            score += 0.1;
        }
        return Math.min(score, 1.0);
    }

    private double matchWeight(String field, String keyword, double weight) {
        if (!hasText(field) || !hasText(keyword)) {
            return 0.0;
        }
        return field.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)) ? weight : 0.0;
    }

    private boolean shareToken(String left, String right) {
        if (!hasText(left) || !hasText(right)) {
            return false;
        }

        Set<String> tokens = new HashSet<>(extractTokens(left));
        for (String token : extractTokens(right)) {
            if (tokens.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean textEquals(String left, String right) {
        return hasText(left) && hasText(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private double recencyWeight(LocalDateTime actionTime) {
        if (actionTime == null) {
            return 0.0;
        }

        long days = Math.max(0, Duration.between(actionTime, LocalDateTime.now()).toDays());
        if (days <= 3) {
            return 1.0;
        }
        if (days <= 7) {
            return 0.8;
        }
        if (days <= 14) {
            return 0.5;
        }
        if (days <= 30) {
            return 0.2;
        }
        return 0.0;
    }

    private double frequencyWeight(int count) {
        return Math.min(1.0, Math.log1p(Math.max(1, count)));
    }

    private LocalDateTime latest(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1) {
            throw new AppException(400, "size must be at least 1.");
        }
        return Math.min(size, MAX_SIZE);
    }

    private double roundScore(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer safeNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private String firstNonBlank(String first, String second) {
        if (hasText(first)) {
            return first;
        }
        return hasText(second) ? second : null;
    }

    private record ProfileMatch(
            double score,
            Set<String> reasons
    ) {
    }

    private final class ProfileCandidateScore {
        private final PolicyList policy;
        private final Set<String> reasons = new LinkedHashSet<>();
        private double totalScore;

        private ProfileCandidateScore(PolicyList policy) {
            this.policy = policy;
        }

        private PolicyList policy() {
            return policy;
        }

        private double totalScore() {
            return totalScore;
        }

        private void add(ProfileMatch match) {
            totalScore = Math.max(totalScore, match.score());
            reasons.addAll(match.reasons());
        }

        private String buildReason() {
            if (reasons.isEmpty()) {
                return "마이페이지 정보를 바탕으로 추천했어요.";
            }

            List<String> topReasons = reasons.stream()
                    .limit(3)
                    .toList();
            return String.join(", ", topReasons) + "가 맞아 추천했어요.";
        }
    }

    private final class CandidateScore {
        private final PolicyList policy;
        private final Set<String> keywordMatches = new LinkedHashSet<>();
        private final Set<String> similarServiceTypes = new LinkedHashSet<>();
        private double keywordScore;
        private double viewScore;
        private double recencyScore;
        private double frequencyScore;
        private double totalScore;

        private CandidateScore(PolicyList policy) {
            this.policy = policy;
        }

        private PolicyList policy() {
            return policy;
        }

        private double totalScore() {
            return totalScore;
        }

        private void finalizeScore(double maxViewCount) {
            double normalizedKeyword = clamp(keywordScore);
            double normalizedView = clamp(viewScore);
            double normalizedRecency = clamp(recencyScore);
            double normalizedFrequency = clamp(frequencyScore);
            double popularityBoost = normalizePopularity(policy.getViewCount(), maxViewCount);

            totalScore = (KEYWORD_WEIGHT * normalizedKeyword)
                    + (VIEW_WEIGHT * normalizedView)
                    + (RECENCY_WEIGHT * normalizedRecency)
                    + (FREQUENCY_WEIGHT * normalizedFrequency)
                    + (POPULARITY_WEIGHT * popularityBoost);
        }

        private String buildReason() {
            if (!keywordMatches.isEmpty()) {
                String keyword = keywordMatches.iterator().next();
                return "Recommended because it matches your recent interest in '" + keyword + "'.";
            }
            if (!similarServiceTypes.isEmpty()) {
                String type = similarServiceTypes.iterator().next();
                return "Recommended because it is similar to policies you recently viewed in " + type + ".";
            }
            return "Recommended based on your recent policy activity.";
        }

        private double clamp(double value) {
            return Math.min(1.0, Math.max(0.0, value));
        }

        private double normalizePopularity(Integer viewCount, double maxViewCount) {
            if (viewCount == null || viewCount <= 0 || maxViewCount <= 0) {
                return 0.0;
            }
            return Math.min(1.0, viewCount / maxViewCount);
        }
    }
}
