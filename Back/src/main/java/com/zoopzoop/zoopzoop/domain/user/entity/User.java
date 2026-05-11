package com.zoopzoop.zoopzoop.domain.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Builder
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String profileImageUrl;

    // 상세 프로필 필드들
    private Integer age;

    @Column(length = 10)
    private String gender; // MALE, FEMALE

    @Column(length = 100)
    private String region; // 시/도

    @Column(length = 100)
    private String district; // 구/군

    @Column(length = 20)
    private String maritalStatus; // SINGLE, MARRIED

    @Column(length = 50)
    private String employmentStatus; // EMPLOYED, UNEMPLOYED 등

    private Integer householdSize;
    private Integer income;
    private Integer incomeBracket; // 소득 구간 (1~10구간)

    // 🚀 DB의 대소문자 혼용 문제를 해결하기 위한 유연한 Converter 적용
    @Convert(converter = RoleConverter.class)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 통합 업데이트 메서드
    public void updateProfile(String name, String email, String profileImageUrl,
                              Integer age, String gender, String region, String district,
                              String maritalStatus, String employmentStatus,
                              Integer householdSize, Integer income, Integer incomeBracket) {
        if (name != null) this.name = name;
        if (email != null) this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.age = age;
        this.gender = gender;
        this.region = region;
        this.district = district;
        this.maritalStatus = maritalStatus;
        this.employmentStatus = employmentStatus;
        this.householdSize = householdSize;
        this.income = income;
        this.incomeBracket = incomeBracket;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}