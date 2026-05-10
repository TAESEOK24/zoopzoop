package com.zoopzoop.zoopzoop.domain.notification.service;

import com.zoopzoop.zoopzoop.domain.notification.dto.NotificationListResponse;
import com.zoopzoop.zoopzoop.domain.notification.dto.NotificationResponse;
import com.zoopzoop.zoopzoop.domain.notification.dto.NotificationSettingsRequest;
import com.zoopzoop.zoopzoop.domain.notification.dto.NotificationSettingsResponse;
import com.zoopzoop.zoopzoop.domain.notification.dto.UnreadCountResponse;
import com.zoopzoop.zoopzoop.domain.notification.entity.Notification;
import com.zoopzoop.zoopzoop.domain.notification.entity.NotificationSetting;
import com.zoopzoop.zoopzoop.domain.notification.entity.NotificationType;
import com.zoopzoop.zoopzoop.domain.notification.repository.NotificationRepository;
import com.zoopzoop.zoopzoop.domain.notification.repository.NotificationSettingRepository;
import com.zoopzoop.zoopzoop.domain.policy.entity.MyScrap;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import com.zoopzoop.zoopzoop.domain.policy.repository.MyScrapRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyListRepository;
import com.zoopzoop.zoopzoop.domain.recommendation.dto.RecommendationItemResponse;
import com.zoopzoop.zoopzoop.domain.recommendation.service.RecommendationService;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import com.zoopzoop.zoopzoop.global.security.AuthenticatedUser;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final int DEADLINE_WINDOW_DAYS = 7;
    private static final int DAILY_NOTIFICATION_LIMIT = 3;
    private static final int RECOMMENDED_NOTIFICATION_COOLDOWN_DAYS = 7;

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final MyScrapRepository myScrapRepository;
    private final PolicyListRepository policyListRepository;
    private final UserRepository userRepository;
    private final PolicyDeadlineParser deadlineParser;
    private final RecommendationService recommendationService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationSettingRepository notificationSettingRepository,
            MyScrapRepository myScrapRepository,
            PolicyListRepository policyListRepository,
            UserRepository userRepository,
            PolicyDeadlineParser deadlineParser,
            RecommendationService recommendationService
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationSettingRepository = notificationSettingRepository;
        this.myScrapRepository = myScrapRepository;
        this.policyListRepository = policyListRepository;
        this.userRepository = userRepository;
        this.deadlineParser = deadlineParser;
        this.recommendationService = recommendationService;
    }

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("notification", "notification module ready");
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getRecentNotifications(AuthenticatedUser currentUser, int size) {
        Long userId = requireUserId(currentUser);
        int boundedSize = Math.min(Math.max(size, 1), 20);
        List<NotificationResponse> items = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, boundedSize))
                .getContent()
                .stream()
                .map(NotificationResponse::from)
                .toList();

        return new NotificationListResponse(items);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(AuthenticatedUser currentUser) {
        Long userId = requireUserId(currentUser);
        return new UnreadCountResponse(notificationRepository.countByUserIdAndReadStatusFalse(userId));
    }

    @Transactional
    public NotificationResponse markRead(AuthenticatedUser currentUser, Long notificationId) {
        Long userId = requireUserId(currentUser);
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new AppException(404, "Notification not found."));
        notification.markRead();

        return NotificationResponse.from(notification);
    }

    @Transactional
    public UnreadCountResponse markAllRead(AuthenticatedUser currentUser) {
        Long userId = requireUserId(currentUser);
        notificationRepository.markAllRead(userId);
        return new UnreadCountResponse(0);
    }

    @Transactional
    public NotificationSettingsResponse getSettings(AuthenticatedUser currentUser) {
        Long userId = requireUserId(currentUser);
        NotificationSetting setting = getOrCreateSetting(userId);
        return NotificationSettingsResponse.from(setting);
    }

    @Transactional
    public NotificationSettingsResponse updateSettings(
            AuthenticatedUser currentUser,
            NotificationSettingsRequest request
    ) {
        Long userId = requireUserId(currentUser);
        NotificationSetting setting = getOrCreateSetting(userId);
        setting.update(
                request.deadlineSoon(),
                request.newPolicy(),
                request.recommendedPolicy(),
                request.browser(),
                request.email()
        );

        return NotificationSettingsResponse.from(setting);
    }

    @Transactional
    public int createDeadlineSoonNotifications() {
        List<Long> userIds = notificationSettingRepository.findByDeadlineSoonEnabledTrue().stream()
                .map(setting -> setting.getUser().getId())
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        Map<Long, List<DeadlineCandidate>> candidatesByUser = myScrapRepository.findByUserIdsWithPolicy(userIds).stream()
                .map(scrap -> toDeadlineCandidate(scrap, today))
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(candidate -> candidate.user().getId()));

        int createdCount = 0;

        for (Map.Entry<Long, List<DeadlineCandidate>> entry : candidatesByUser.entrySet()) {
            List<DeadlineCandidate> candidates = entry.getValue().stream()
                    .sorted(Comparator.comparing(DeadlineCandidate::deadline))
                    .limit(DAILY_NOTIFICATION_LIMIT)
                    .toList();

            for (DeadlineCandidate candidate : candidates) {
                if (notificationRepository.existsByUserIdAndPolicyServiceIdAndType(
                        candidate.user().getId(),
                        candidate.policy().getServiceId(),
                        NotificationType.DEADLINE_SOON
                )) {
                    continue;
                }

                notificationRepository.save(Notification.builder()
                        .user(candidate.user())
                        .policy(candidate.policy())
                        .policyName(candidate.policy().getServiceName())
                        .type(NotificationType.DEADLINE_SOON)
                        .title("신청 마감 임박 정책")
                        .message(buildDeadlineMessage(candidate.policy(), candidate.daysLeft()))
                        .readStatus(false)
                        .read(false)
                        .build());
                createdCount++;
            }
        }

        return createdCount;
    }

    @Transactional
    public int createNewPolicyNotifications() {
        List<User> users = notificationSettingRepository.findByNewPolicyEnabledTrue().stream()
                .map(NotificationSetting::getUser)
                .toList();

        if (users.isEmpty()) {
            return 0;
        }

        List<PolicyList> recentPolicies = policyListRepository.findByCreatedAtAfterOrderByCreatedAtDesc(
                LocalDateTime.now().minusHours(24),
                PageRequest.of(0, DAILY_NOTIFICATION_LIMIT)
        );

        if (recentPolicies.isEmpty()) {
            return 0;
        }

        int createdCount = 0;

        for (User user : users) {
            for (PolicyList policy : recentPolicies) {
                if (notificationRepository.existsByUserIdAndPolicyServiceIdAndType(
                        user.getId(),
                        policy.getServiceId(),
                        NotificationType.NEW_POLICY
                )) {
                    continue;
                }

                notificationRepository.save(Notification.builder()
                        .user(user)
                        .policy(policy)
                        .policyName(policy.getServiceName())
                        .type(NotificationType.NEW_POLICY)
                        .title("신규 등록 정책")
                        .message("%s 정책이 새로 등록되었습니다.".formatted(policy.getServiceName()))
                        .readStatus(false)
                        .read(false)
                        .build());
                createdCount++;
            }
        }

        return createdCount;
    }

    @Transactional
    public int createRecommendedPolicyNotifications() {
        List<User> users = notificationSettingRepository.findByRecommendedPolicyEnabledTrue().stream()
                .map(NotificationSetting::getUser)
                .toList();

        int createdCount = 0;
        LocalDateTime cooldownStart = LocalDateTime.now().minusDays(RECOMMENDED_NOTIFICATION_COOLDOWN_DAYS);

        for (User user : users) {
            if (notificationRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                    user.getId(),
                    NotificationType.RECOMMENDED_POLICY,
                    cooldownStart
            )) {
                continue;
            }

            AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole().name()
            );

            List<RecommendationItemResponse> recommendations = recommendationService
                    .getPersonalizedRecommendations(authenticatedUser, DAILY_NOTIFICATION_LIMIT)
                    .items();

            for (RecommendationItemResponse recommendation : recommendations) {
                if (notificationRepository.existsByUserIdAndPolicyServiceIdAndType(
                        user.getId(),
                        recommendation.serviceId(),
                        NotificationType.RECOMMENDED_POLICY
                )) {
                    continue;
                }

                PolicyList policy = policyListRepository.findById(recommendation.serviceId()).orElse(null);
                if (policy == null) {
                    continue;
                }

                notificationRepository.save(Notification.builder()
                        .user(user)
                        .policy(policy)
                        .policyName(policy.getServiceName())
                        .type(NotificationType.RECOMMENDED_POLICY)
                        .title("개인 맞춤 추천 정책")
                        .message("최근 관심사를 바탕으로 %s 정책을 추천합니다.".formatted(policy.getServiceName()))
                        .readStatus(false)
                        .read(false)
                        .build());
                createdCount++;
            }
        }

        return createdCount;
    }

    private List<DeadlineCandidate> toDeadlineCandidate(MyScrap scrap, LocalDate today) {
        return deadlineParser.parse(scrap.getPolicy().getApplicationDeadline())
                .filter(deadline -> !deadline.isBefore(today))
                .filter(deadline -> !deadline.isAfter(today.plusDays(DEADLINE_WINDOW_DAYS)))
                .map(deadline -> List.of(new DeadlineCandidate(
                        scrap.getUser(),
                        scrap.getPolicy(),
                        deadline,
                        ChronoUnit.DAYS.between(today, deadline)
                )))
                .orElseGet(List::of);
    }

    private String buildDeadlineMessage(PolicyList policy, long daysLeft) {
        if (daysLeft == 0) {
            return "%s 신청 마감일이 오늘입니다.".formatted(policy.getServiceName());
        }

        return "%s 신청 마감이 %d일 남았습니다.".formatted(policy.getServiceName(), daysLeft);
    }

    private NotificationSetting getOrCreateSetting(Long userId) {
        return notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new AppException(401, "Login required."));
                    return notificationSettingRepository.save(NotificationSetting.defaults(user));
                });
    }

    private Long requireUserId(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.id() == null) {
            throw new AppException(401, "Login required.");
        }

        return currentUser.id();
    }

    private record DeadlineCandidate(
            User user,
            PolicyList policy,
            LocalDate deadline,
            long daysLeft
    ) {
    }
}
