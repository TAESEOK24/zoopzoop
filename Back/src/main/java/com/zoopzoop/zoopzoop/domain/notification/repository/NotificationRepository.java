package com.zoopzoop.zoopzoop.domain.notification.repository;

import com.zoopzoop.zoopzoop.domain.notification.entity.Notification;
import com.zoopzoop.zoopzoop.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = "policy")
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadStatusFalse(Long userId);

    boolean existsByUserIdAndPolicyServiceIdAndType(Long userId, String serviceId, NotificationType type);

    boolean existsByUserIdAndTypeAndCreatedAtAfter(Long userId, NotificationType type, LocalDateTime createdAfter);

    @EntityGraph(attributePaths = "policy")
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);

    @Modifying
    @Query("""
            update Notification n
            set n.readStatus = true,
                n.read = true,
                n.readAt = current_timestamp
            where n.user.id = :userId
              and n.readStatus = false
            """)
    int markAllRead(@Param("userId") Long userId);
}
