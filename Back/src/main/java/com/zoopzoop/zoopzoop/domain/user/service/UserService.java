package com.zoopzoop.zoopzoop.domain.user.service;

import com.zoopzoop.zoopzoop.domain.notification.repository.NotificationRepository;
import com.zoopzoop.zoopzoop.domain.notification.repository.NotificationSettingRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.MyScrapRepository;
import com.zoopzoop.zoopzoop.domain.searchlog.repository.SearchLogRepository;
import com.zoopzoop.zoopzoop.domain.user.dto.UserSummary;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final MyScrapRepository myScrapRepository;
    private final SearchLogRepository searchLogRepository;

    public UserService(
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            NotificationSettingRepository notificationSettingRepository,
            MyScrapRepository myScrapRepository,
            SearchLogRepository searchLogRepository
    ) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationSettingRepository = notificationSettingRepository;
        this.myScrapRepository = myScrapRepository;
        this.searchLogRepository = searchLogRepository;
    }

    @Transactional(readOnly = true)
    public UserSummary getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(404, "User not found."));

        return UserSummary.from(user);
    }

    // 🚀 [추가됨] 이메일로 내 정보를 찾는 기능!
    @Transactional(readOnly = true)
    public UserSummary getCurrentUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(404, "User not found with email: " + email));

        return UserSummary.from(user);
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
