package com.zoopzoop.zoopzoop.domain.user.service;

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

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("user", "user module ready");
    }
}