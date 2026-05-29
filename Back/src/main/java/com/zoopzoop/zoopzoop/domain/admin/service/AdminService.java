package com.zoopzoop.zoopzoop.domain.admin.service;

import com.zoopzoop.zoopzoop.domain.admin.dto.*;
import com.zoopzoop.zoopzoop.domain.community.entity.Report;
import com.zoopzoop.zoopzoop.domain.community.repository.CommentRepository;
import com.zoopzoop.zoopzoop.domain.community.repository.PostRepository;
import com.zoopzoop.zoopzoop.domain.community.repository.ReportRepository;
import com.zoopzoop.zoopzoop.domain.community.service.CommunityService;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyListRepository;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySyncService;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import com.zoopzoop.zoopzoop.domain.user.service.UserService;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
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
    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;

    public AdminService(UserRepository userRepository, UserService userService,
                        PostRepository postRepository, CommunityService communityService,
                        PolicyListRepository policyListRepository, PolicySyncService policySyncService,
                        ReportRepository reportRepository, CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.postRepository = postRepository;
        this.communityService = communityService;
        this.policyListRepository = policyListRepository;
        this.policySyncService = policySyncService;
        this.reportRepository = reportRepository;
        this.commentRepository = commentRepository;
    }

    public HealthCheckDto getStatus() {
        return new HealthCheckDto("admin", "admin module ready");
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalPolicies = policyListRepository.count();

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        long todayPosts = postRepository.findAll().stream()
                .filter(post -> today.equals(post.getDate()))
                .count();

        return new AdminDashboardResponse(totalUsers, todayPosts, totalPolicies);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(AdminUserResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public void forceDeleteUser(Long targetUserId) {
        userService.withdraw(targetUserId);
    }

    @Transactional
    public AdminUserResponse grantAdminRole(Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(404, "User not found."));

        user.grantAdminRole();
        return AdminUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<AdminPostResponse> getAllPosts() {
        return postRepository.findAll().stream().map(AdminPostResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public void forceDeletePost(Long postId) {
        communityService.deletePost(postId);
    }

    public String syncPolicies() {
        return policySyncService.syncFullData();
    }

    // 🚨 신고 관리 기능
    @Transactional(readOnly = true)
    public List<AdminReportResponse> getPendingReports() {
        return reportRepository.findByStatusOrderByIdDesc("PENDING").stream()
                .map(AdminReportResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void resolveReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고 내역이 없습니다."));
        report.resolve();
    }

    @Transactional
    public void forceDeleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}
