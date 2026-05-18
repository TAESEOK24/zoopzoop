package com.zoopzoop.zoopzoop.domain.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyConditions;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyConditionsRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyListRepository;
import com.zoopzoop.zoopzoop.domain.recommendation.dto.ProfileRecommendationResponse;
import com.zoopzoop.zoopzoop.domain.recommendation.dto.RecommendationResponse;
import com.zoopzoop.zoopzoop.domain.searchlog.entity.SearchLog;
import com.zoopzoop.zoopzoop.domain.searchlog.repository.SearchLogRepository;
import com.zoopzoop.zoopzoop.domain.user.entity.Role;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import com.zoopzoop.zoopzoop.global.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private SearchLogRepository searchLogRepository;

    @Mock
    private PolicyListRepository policyListRepository;

    @Mock
    private PolicyConditionsRepository policyConditionsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void returnsPopularPoliciesWhenNoLogsExist() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "user@example.com", "User", "USER");
        PolicyList policy = policy("svc-1", "Popular Policy", "Housing", "Youth Support Office", 200);

        when(searchLogRepository.findTop30ByUserIdAndActionTypeOrderByActionTimeDesc(1, "SEARCH")).thenReturn(List.of());
        when(searchLogRepository.findTop30ByUserIdAndActionTypeOrderByActionTimeDesc(1, "VIEW")).thenReturn(List.of());
        when(policyListRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(policy)));

        RecommendationResponse response = recommendationService.getPersonalizedRecommendations(user, 6);

        assertEquals(1, response.items().size());
        assertEquals("svc-1", response.items().get(0).serviceId());
    }

    @Test
    void scoresKeywordAndViewSignalsAndExcludesViewedPolicy() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "user@example.com", "User", "USER");
        SearchLog searchLog = SearchLog.builder()
                .userId(1)
                .keyword("youth housing")
                .actionType("SEARCH")
                .actionTime(LocalDateTime.now())
                .build();
        SearchLog viewLog = SearchLog.builder()
                .userId(1)
                .serviceId("viewed-1")
                .actionType("VIEW")
                .actionTime(LocalDateTime.now())
                .build();

        PolicyList viewedPolicy = policy("viewed-1", "Youth Monthly Rent", "Housing", "Seoul Youth Team", 100);
        PolicyList recommendedPolicy = policy("svc-2", "Youth Housing Stability", "Housing", "Seoul Youth Team", 80);

        when(searchLogRepository.findTop30ByUserIdAndActionTypeOrderByActionTimeDesc(1, "SEARCH")).thenReturn(List.of(searchLog));
        when(searchLogRepository.findTop30ByUserIdAndActionTypeOrderByActionTimeDesc(1, "VIEW")).thenReturn(List.of(viewLog));
        when(policyListRepository.searchByKeyword(eq("youth housing"), any(Pageable.class))).thenReturn(List.of(recommendedPolicy));
        when(policyListRepository.searchByKeyword(eq("youth"), any(Pageable.class))).thenReturn(List.of(recommendedPolicy));
        when(policyListRepository.searchByKeyword(eq("housing"), any(Pageable.class))).thenReturn(List.of(recommendedPolicy));
        when(policyListRepository.findById("viewed-1")).thenReturn(Optional.of(viewedPolicy));
        when(policyListRepository.findByServiceTypeContainingIgnoreCase(eq("Housing"), any(Pageable.class)))
                .thenReturn(List.of(viewedPolicy, recommendedPolicy));
        when(policyListRepository.searchByOrganization(eq("Seoul Youth Team"), any(Pageable.class)))
                .thenReturn(List.of(viewedPolicy, recommendedPolicy));

        RecommendationResponse response = recommendationService.getPersonalizedRecommendations(user, 6);

        assertEquals(1, response.items().size());
        assertEquals("svc-2", response.items().get(0).serviceId());
        assertFalse(response.items().get(0).reason().isBlank());
    }

    @Test
    void rejectsMissingAuthentication() {
        AppException exception = assertThrows(AppException.class,
                () -> recommendationService.getPersonalizedRecommendations(null, 6));

        assertEquals(401, exception.statusCode());
    }

    @Test
    void profileBasedRecommendationsScoreUserProfileSignals() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "user@example.com", "Jake Song", "USER");
        User profile = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded")
                .name("Jake Song")
                .role(Role.USER)
                .age(23)
                .gender("MALE")
                .region("경기도")
                .district("안산시")
                .maritalStatus("SINGLE")
                .employmentStatus("STUDENT")
                .householdSize(3)
                .income(1)
                .incomeBracket(1)
                .build();
        PolicyList matchedPolicy = policy("svc-profile", "안산시 대학생 생활안정 지원", "생활지원", "안산시 복지과", 50);
        PolicyConditions matchedConditions = PolicyConditions.builder()
                .serviceId("svc-profile")
                .serviceName("안산시 대학생 생활안정 지원")
                .ja0110(19)
                .ja0111(34)
                .ja0101("Y")
                .ja0201("Y")
                .ja0320("Y")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(policyListRepository.searchByKeyword(any(String.class), any(Pageable.class))).thenReturn(List.of(matchedPolicy));
        when(policyListRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(matchedPolicy)));
        when(policyConditionsRepository.findById("svc-profile")).thenReturn(Optional.of(matchedConditions));

        ProfileRecommendationResponse response = recommendationService.getProfileBasedRecommendations(authenticatedUser, 3);

        assertTrue(response.profileReady());
        assertEquals(1, response.items().size());
        assertEquals("svc-profile", response.items().get(0).serviceId());
        assertTrue(response.items().get(0).reason().contains("나이"));
    }

    @Test
    void profileBasedRecommendationsKeepInternalAdministrativeCharactersWhenCompactingRegion() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "user@example.com", "User", "USER");
        User profile = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded")
                .name("User")
                .role(Role.USER)
                .district("시흥시")
                .build();
        PolicyList matchedPolicy = policy("svc-region", "시흥 청년 지원", "생활지원", "시흥시", 10);

        when(userRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(policyListRepository.searchByKeyword(eq("시흥시"), any(Pageable.class))).thenReturn(List.of());
        when(policyListRepository.searchByKeyword(eq("시흥"), any(Pageable.class))).thenReturn(List.of(matchedPolicy));
        when(policyListRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(policyConditionsRepository.findById("svc-region")).thenReturn(Optional.empty());

        ProfileRecommendationResponse response = recommendationService.getProfileBasedRecommendations(authenticatedUser, 3);

        assertTrue(response.profileReady());
        assertEquals(1, response.items().size());
        assertEquals("svc-region", response.items().get(0).serviceId());
    }

    @Test
    void profileBasedRecommendationsReturnNotReadyWhenProfileIsEmpty() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "user@example.com", "User", "USER");
        User profile = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded")
                .name("User")
                .role(Role.USER)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(profile));

        ProfileRecommendationResponse response = recommendationService.getProfileBasedRecommendations(authenticatedUser, 3);

        assertFalse(response.profileReady());
        assertTrue(response.items().isEmpty());
    }

    private PolicyList policy(String serviceId, String serviceName, String serviceType, String orgName, int viewCount) {
        return PolicyList.builder()
                .serviceId(serviceId)
                .serviceName(serviceName)
                .serviceType(serviceType)
                .purposeSummary("Support for " + serviceName)
                .target("Youth")
                .supportContent("Housing support")
                .applicationMethod("Online")
                .applicationDeadline("Always")
                .orgName(orgName)
                .departmentName(orgName)
                .viewCount(viewCount)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
