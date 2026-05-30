package com.zoopzoop.zoopzoop.domain.notification.entity;

import com.zoopzoop.zoopzoop.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@Table(name = "notification_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private boolean deadlineSoonEnabled;

    @Column(nullable = false)
    private boolean newPolicyEnabled;

    @Column(nullable = false)
    private boolean recommendedPolicyEnabled;

    @Column(nullable = false)
    private boolean quietHoursEnabled;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static NotificationSetting defaults(User user) {
        return NotificationSetting.builder()
                .user(user)
                .deadlineSoonEnabled(true)
                .newPolicyEnabled(true)
                .recommendedPolicyEnabled(true)
                .quietHoursEnabled(false)
                .build();
    }

    public void update(
            boolean deadlineSoonEnabled,
            boolean newPolicyEnabled,
            boolean recommendedPolicyEnabled
    ) {
        this.deadlineSoonEnabled = deadlineSoonEnabled;
        this.newPolicyEnabled = newPolicyEnabled;
        this.recommendedPolicyEnabled = recommendedPolicyEnabled;
        this.quietHoursEnabled = false;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
