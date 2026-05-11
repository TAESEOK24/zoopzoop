package com.zoopzoop.zoopzoop.domain.user.service;

import com.zoopzoop.zoopzoop.domain.notification.repository.NotificationRepository;
import com.zoopzoop.zoopzoop.domain.notification.repository.NotificationSettingRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.MyScrapRepository;
import com.zoopzoop.zoopzoop.domain.searchlog.repository.SearchLogRepository;
import com.zoopzoop.zoopzoop.domain.user.dto.UserSummary;
import com.zoopzoop.zoopzoop.domain.user.dto.UserUpdateRequest; // 🚀 DTO 임포트
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final MyScrapRepository myScrapRepository;
    private final SearchLogRepository searchLogRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            NotificationSettingRepository notificationSettingRepository,
            MyScrapRepository myScrapRepository,
            SearchLogRepository searchLogRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationSettingRepository = notificationSettingRepository;
        this.myScrapRepository = myScrapRepository;
        this.searchLogRepository = searchLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserSummary getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(404, "User not found."));

        return UserSummary.from(user);
    }

    @Transactional(readOnly = true)
    public UserSummary getCurrentUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(404, "User not found with email: " + email));

        return UserSummary.from(user);
    }

    // 🚀 [고도화됨] 프로필 통합 수정 (이름, 이메일, 이미지)
    @Transactional
    public void updateProfile(Long userId, UserUpdateRequest.Profile request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(404, "사용자를 찾을 수 없습니다."));

        // 프론트엔드에서 데이터가 넘어왔을 때만 변경하도록 안전장치 설정
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.updateName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            user.updateEmail(request.getEmail());
        }
        if (request.getProfileImageUrl() != null) {
            user.updateProfileImage(request.getProfileImageUrl());
        }
    }

    // 비밀번호 변경
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(404, "사용자를 찾을 수 없습니다."));

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AppException(400, "현재 비밀번호가 일치하지 않습니다.");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void withdraw(Long userId) {
        if (userId == null) {
            throw new AppException(401, "Authentication is required.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(404, "User not found."));

        notificationRepository.deleteByUserId(userId);
        notificationSettingRepository.deleteByUserId(userId);
        myScrapRepository.deleteByUserId(userId);
        searchLogRepository.deleteByUserId(Math.toIntExact(userId));
        userRepository.delete(user);
    }

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("user", "user module ready");
    }
}