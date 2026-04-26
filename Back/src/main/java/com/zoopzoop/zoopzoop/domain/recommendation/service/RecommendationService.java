package com.zoopzoop.zoopzoop.domain.recommendation.service;

import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyListRepository;
import com.zoopzoop.zoopzoop.domain.recommendation.dto.RecommendationItemResponse;
import com.zoopzoop.zoopzoop.domain.recommendation.dto.RecommendationResponse;
import com.zoopzoop.zoopzoop.domain.searchlog.entity.SearchLog;
import com.zoopzoop.zoopzoop.domain.searchlog.repository.SearchLogRepository;
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
    private static final double KEYWORD_WEIGHT = 0.40;
    private static final double VIEW_WEIGHT = 0.30;
    private static final double RECENCY_WEIGHT = 0.15;
    private static final double FREQUENCY_WEIGHT = 0.10;
    private static final double ALREADY_VIEWED_PENALTY = 0.20;
    private static final double POPULARITY_WEIGHT = 0.05;

    private final SearchLogRepository searchLogRepository;
    private final PolicyListRepository policyListRepository;

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

    private String firstNonBlank(String first, String second) {
        if (hasText(first)) {
            return first;
        }
        return hasText(second) ? second : null;
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
