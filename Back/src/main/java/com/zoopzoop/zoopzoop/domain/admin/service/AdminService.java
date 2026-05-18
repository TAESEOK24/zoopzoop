package com.zoopzoop.zoopzoop.domain.admin.service;

import com.zoopzoop.zoopzoop.domain.admin.dto.*;
import com.zoopzoop.zoopzoop.domain.community.repository.PostRepository;
import com.zoopzoop.zoopzoop.domain.community.service.CommunityService;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyListRepository;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySyncService;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import com.zoopzoop.zoopzoop.domain.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PostRepository postRepository;
    private final CommunityService communityService;
    private final PolicyListRepository policyListRepository;
    private final PolicySyncService policySyncService;

    public AdminService(UserRepository userRepository, UserService userService,
                        PostRepository postRepository, CommunityService communityService,
                        PolicyListRepository policyListRepository, PolicySyncService policySyncService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.postRepository = postRepository;
        this.communityService = communityService;
        this.policyListRepository = policyListRepository;
        this.policySyncService = policySyncService;
    }

    // 📊 1. 대시보드 통계 데이터 가져오기
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalPolicies = policyListRepository.count();

        // 오늘 작성된 게시글 수 계산 ("yyyy.MM.dd" 형식과 비교)
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        long todayPosts = postRepository.findAll().stream()
                .filter(post -> today.equals(post.getDate()))
                .count();

        return new AdminDashboardResponse(totalUsers, todayPosts, totalPolicies);
    }

    // 👥 2. 전체 유저 목록 가져오기
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(AdminUserResponse::from)
                .collect(Collectors.toList());
    }

    // 🚫 3. 유저 강제 탈퇴
    @Transactional
    public void forceDeleteUser(Long targetUserId) {
        userService.withdraw(targetUserId);
    }

    // 📝 4. 전체 커뮤니티 게시글 가져오기
    @Transactional(readOnly = true)
    public List<AdminPostResponse> getAllPosts() {
        return postRepository.findAll().stream()
                .map(AdminPostResponse::from)
                .collect(Collectors.toList());
    }

    // 🗑️ 5. 커뮤니티 게시글 강제 삭제
    @Transactional
    public void forceDeletePost(Long postId) {
        // 기존에 만들어둔 서비스 삭제 로직 재사용
        communityService.deletePost(postId);
    }

    // ⚙️ 6. 공공데이터포털 정책 강제 동기화 (Sync)
    public String syncPolicies() {
        return policySyncService.syncFullData();
    }
}