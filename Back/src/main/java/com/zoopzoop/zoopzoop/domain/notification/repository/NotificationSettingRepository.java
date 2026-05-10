package com.zoopzoop.zoopzoop.domain.notification.repository;

import com.zoopzoop.zoopzoop.domain.notification.entity.NotificationSetting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<NotificationSetting> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    List<NotificationSetting> findByDeadlineSoonEnabledTrue();

    @EntityGraph(attributePaths = "user")
    List<NotificationSetting> findByNewPolicyEnabledTrue();

    @EntityGraph(attributePaths = "user")
    List<NotificationSetting> findByRecommendedPolicyEnabledTrue();
}
