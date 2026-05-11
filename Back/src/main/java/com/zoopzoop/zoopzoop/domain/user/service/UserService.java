package com.zoopzoop.zoopzoop.domain.user.service;

import com.zoopzoop.zoopzoop.domain.notification.repository.NotificationRepository;
import com.zoopzoop.zoopzoop.domain.notification.repository.NotificationSettingRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.MyScrapRepository;
import com.zoopzoop.zoopzoop.domain.searchlog.repository.SearchLogRepository;
import com.zoopzoop.zoopzoop.domain.user.dto.UserSummary;
import com.zoopzoop.zoopzoop.domain.user.dto.UserUpdateRequest;
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

    /**
     * 2026년 가구원 수별 기준 중위소득 100% 금액 (월 단위, 원)
     */
    private double getMedianIncome2026(int householdSize) {
        return switch (householdSize) {
            case 1 -> 2564238.0;
            case 2 -> 4199292.0;
            case 3 -> 5359036.0;
            case 4 -> 6494738.0;
            case 5 -> 7556719.0;
            case 6 -> 8555952.0;
            case 7 -> 9515150.0;
            default -> 9515150.0 + ((householdSize - 7) * 959198.0);
        };
    }

    /**
     * 중위소득 백분율을 기준으로 소득 구간(1~10) 결정
     */
    private Integer convertToBracket(double percent) {
        if (percent <= 30) return 1;
        if (percent <= 50) return 2;
        if (percent <= 70) return 3;
        if (percent <= 90) return 4;
        if (percent <= 100) return 5;
        if (percent <= 130) return 6;
        if (percent <= 150) return 7;
        if (percent <= 200) return 8;
        if (percent <= 300) return 9;
        return 10;
    }

    /**
     * 가구원 수와 연 소득을 바탕으로 소득 구간을 자동 계산
     */
    private Integer calculateIncomeBracket(Integer householdSize, Integer annualIncome) {
        if (householdSize == null || annualIncome == null || householdSize <= 0) {
            return null;
        }

        // 연 소득(만원) -> 월 소득(원) 변환
        double monthlyIncomeWon = (annualIncome * 10000.0) / 12.0;
        double baseMedian = getMedianIncome2026(householdSize);
        double percent = (monthlyIncomeWon / baseMedian) * 100;

        return convertToBracket(percent);
    }

    /**
     * 프로필 및 맞춤 정보 업데이트
     */
    @Transactional
    public void updateProfile(Long userId, UserUpdateRequest.Profile request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(404, "사용자를 찾을 수 없습니다."));

        // 소득 구간 자동 계산 (사용자 직접 수정 불가)
        Integer autoCalculatedBracket = calculateIncomeBracket(request.getHouseholdSize(), request.getIncome());

        user.updateProfile(
                request.getName(),
                request.getEmail(),
                request.getProfileImageUrl(),
                request.getAge(),
                request.getGender(),
                request.getRegion(),
                request.getDistrict(),
                request.getMaritalStatus(),
                request.getEmploymentStatus(),
                request.getHouseholdSize(),
                request.getIncome(),
                autoCalculatedBracket
        );
    }

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