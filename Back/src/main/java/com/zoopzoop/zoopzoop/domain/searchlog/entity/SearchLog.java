package com.zoopzoop.zoopzoop.domain.searchlog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
@Table(name = "search_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(length = 255)
    private String keyword;

    @Column(name = "service_id", length = 100)
    private String serviceId;

    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;

    @PrePersist
    void onCreate() {
        if (actionTime == null) {
            actionTime = LocalDateTime.now();
        }
    }
}
