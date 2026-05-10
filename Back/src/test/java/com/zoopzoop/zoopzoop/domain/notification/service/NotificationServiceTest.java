package com.zoopzoop.zoopzoop.domain.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zoopzoop.zoopzoop.domain.notification.entity.Notification;
import com.zoopzoop.zoopzoop.domain.notification.entity.NotificationSetting;
import com.zoopzoop.zoopzoop.domain.notification.entity.NotificationType;
import com.zoopzoop.zoopzoop.domain.notification.repository.NotificationRepository;
import com.zoopzoop.zoopzoop.domain.notification.repository.NotificationSettingRepository;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import com.zoopzoop.zoopzoop.domain.policy.repository.MyScrapRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyListRepository;
import com.zoopzoop.zoopzoop.domain.recommendation.dto.RecommendationItemResponse;
import com.zoopzoop.zoopzoop.domain.recommendation.dto.RecommendationResponse;
import com.zoopzoop.zoopzoop.domain.recommendation.service.RecommendationService;
import com.zoopzoop.zoopzoop.domain.user.entity.Role;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private MyScrapRepository myScrapRepository;

    @Mock
    private PolicyListRepository policyListRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PolicyDeadlineParser deadlineParser;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void createsNewPolicyNotificationsForUsersWithSettingEnabled() {
        User user = user(1L);
        PolicyList firstPolicy = policy("svc-1", "첫 번째 신규 정책");
        PolicyList secondPolicy = policy("svc-2", "두 번째 신규 정책");

        when(notificationSettingRepository.findByNewPolicyEnabledTrue())
                .thenReturn(List.of(NotificationSetting.defaults(user)));
        when(policyListRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(firstPolicy, secondPolicy));
        when(notificationRepository.existsByUserIdAndPolicyServiceIdAndType(eq(1L), any(), eq(NotificationType.NEW_POLICY)))
                .thenReturn(false);

        int createdCount = notificationService.createNewPolicyNotifications();

        assertEquals(2, createdCount);
        verify(notificationRepository).save(notificationFor(firstPolicy));
        verify(notificationRepository).save(notificationFor(secondPolicy));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(policyListRepository).findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class), pageableCaptor.capture());
        assertEquals(3, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void skipsNewPolicyNotificationsWhenAlreadyCreated() {
        User user = user(1L);
        PolicyList policy = policy("svc-1", "이미 알림을 보낸 정책");

        when(notificationSettingRepository.findByNewPolicyEnabledTrue())
                .thenReturn(List.of(NotificationSetting.defaults(user)));
        when(policyListRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(policy));
        when(notificationRepository.existsByUserIdAndPolicyServiceIdAndType(1L, "svc-1", NotificationType.NEW_POLICY))
                .thenReturn(true);

        int createdCount = notificationService.createNewPolicyNotifications();

        assertEquals(0, createdCount);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void createsRecommendedPolicyNotificationsFromRecommendationService() {
        User user = user(1L);
        PolicyList policy = policy("svc-1", "맞춤 추천 정책");

        when(notificationSettingRepository.findByRecommendedPolicyEnabledTrue())
                .thenReturn(List.of(NotificationSetting.defaults(user)));
        when(recommendationService.getPersonalizedRecommendations(any(), eq(3)))
                .thenReturn(new RecommendationResponse(List.of(recommendation(policy))));
        when(notificationRepository.existsByUserIdAndPolicyServiceIdAndType(1L, "svc-1", NotificationType.RECOMMENDED_POLICY))
                .thenReturn(false);
        when(policyListRepository.findById("svc-1")).thenReturn(Optional.of(policy));

        int createdCount = notificationService.createRecommendedPolicyNotifications();

        assertEquals(1, createdCount);
        verify(notificationRepository).save(notificationFor(policy, NotificationType.RECOMMENDED_POLICY));
    }

    @Test
    void skipsRecommendedPolicyNotificationsWhenAlreadyCreated() {
        User user = user(1L);
        PolicyList policy = policy("svc-1", "이미 추천 알림을 보낸 정책");

        when(notificationSettingRepository.findByRecommendedPolicyEnabledTrue())
                .thenReturn(List.of(NotificationSetting.defaults(user)));
        when(recommendationService.getPersonalizedRecommendations(any(), eq(3)))
                .thenReturn(new RecommendationResponse(List.of(recommendation(policy))));
        when(notificationRepository.existsByUserIdAndPolicyServiceIdAndType(1L, "svc-1", NotificationType.RECOMMENDED_POLICY))
                .thenReturn(true);

        int createdCount = notificationService.createRecommendedPolicyNotifications();

        assertEquals(0, createdCount);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void skipsRecommendedPolicyNotificationsWhenUserReceivedOneWithinSevenDays() {
        User user = user(1L);

        when(notificationSettingRepository.findByRecommendedPolicyEnabledTrue())
                .thenReturn(List.of(NotificationSetting.defaults(user)));
        when(notificationRepository.existsByUserIdAndTypeAndCreatedAtAfter(eq(1L), eq(NotificationType.RECOMMENDED_POLICY), any(LocalDateTime.class)))
                .thenReturn(true);

        int createdCount = notificationService.createRecommendedPolicyNotifications();

        assertEquals(0, createdCount);
        verify(recommendationService, never()).getPersonalizedRecommendations(any(), any());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .email("user%s@example.com".formatted(id))
                .password("password")
                .name("User " + id)
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private PolicyList policy(String serviceId, String serviceName) {
        return PolicyList.builder()
                .serviceId(serviceId)
                .serviceName(serviceName)
                .serviceType("서비스(일자리)")
                .purposeSummary("신규 정책입니다.")
                .applicationDeadline("상시")
                .orgName("고용노동부")
                .departmentName("청년정책과")
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private RecommendationItemResponse recommendation(PolicyList policy) {
        return RecommendationItemResponse.of(policy, "추천 이유", 0.9);
    }

    private Notification notificationFor(PolicyList policy) {
        return notificationFor(policy, NotificationType.NEW_POLICY);
    }

    private Notification notificationFor(PolicyList policy, NotificationType type) {
        return org.mockito.ArgumentMatchers.argThat(notification ->
                notification.getPolicy().getServiceId().equals(policy.getServiceId())
                        && notification.getType() == type
                        && !notification.isReadStatus()
        );
    }
}
