package com.zoopzoop.zoopzoop.domain.policy.service;

import com.zoopzoop.zoopzoop.domain.policy.dto.MyScrapIdsResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.MyScrapPageResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySummaryResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.ScrapStatusResponse;
import com.zoopzoop.zoopzoop.domain.policy.entity.MyScrap;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import com.zoopzoop.zoopzoop.domain.policy.repository.MyScrapRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyListRepository;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import com.zoopzoop.zoopzoop.global.security.AuthenticatedUser;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyScrapService {

    private final MyScrapRepository myScrapRepository;
    private final PolicyListRepository policyListRepository;
    private final UserRepository userRepository;

    public MyScrapService(
            MyScrapRepository myScrapRepository,
            PolicyListRepository policyListRepository,
            UserRepository userRepository
    ) {
        this.myScrapRepository = myScrapRepository;
        this.policyListRepository = policyListRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ScrapStatusResponse addScrap(AuthenticatedUser currentUser, String serviceId) {
        Long userId = requireUserId(currentUser);

        if (myScrapRepository.existsScrap(userId, serviceId)) {
            return new ScrapStatusResponse(true);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(401, "Login required."));
        PolicyList policy = policyListRepository.findById(serviceId)
                .orElseThrow(() -> new AppException(404, "Policy not found."));

        myScrapRepository.save(MyScrap.builder()
                .user(user)
                .policy(policy)
                .build());

        return new ScrapStatusResponse(true);
    }

    @Transactional
    public ScrapStatusResponse removeScrap(AuthenticatedUser currentUser, String serviceId) {
        Long userId = requireUserId(currentUser);

        myScrapRepository.deleteScrap(userId, serviceId);

        return new ScrapStatusResponse(false);
    }

    @Transactional(readOnly = true)
    public ScrapStatusResponse getScrapStatus(AuthenticatedUser currentUser, String serviceId) {
        Long userId = requireUserId(currentUser);
        return new ScrapStatusResponse(myScrapRepository.existsScrap(userId, serviceId));
    }

    @Transactional(readOnly = true)
    public MyScrapIdsResponse getMyScrapIds(AuthenticatedUser currentUser) {
        Long userId = requireUserId(currentUser);
        return new MyScrapIdsResponse(myScrapRepository.findPolicyIdsByUserId(userId));
    }

    @Transactional(readOnly = true)
    public MyScrapPageResponse getMyScraps(AuthenticatedUser currentUser, String query, int page, int size) {
        Long userId = requireUserId(currentUser);
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size));
        String normalizedQuery = normalizeQuery(query);
        Page<MyScrap> resultPage = findScraps(userId, normalizedQuery, pageable);

        List<PolicySummaryResponse> items = resultPage.getContent().stream()
                .map(MyScrap::getPolicy)
                .map(PolicySummaryResponse::from)
                .toList();

        return new MyScrapPageResponse(
                items,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resultPage.hasNext()
        );
    }

    private Long requireUserId(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.id() == null) {
            throw new AppException(401, "Login required.");
        }

        return currentUser.id();
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 5;
        }

        return Math.min(size, 50);
    }

    private String normalizeQuery(String query) {
        return query == null || query.trim().isEmpty() ? null : query.trim().toLowerCase(Locale.ROOT);
    }

    private Page<MyScrap> findScraps(Long userId, String query, Pageable pageable) {
        return query == null
                ? myScrapRepository.findMyScrapsByRecent(userId, pageable)
                : myScrapRepository.searchMyScrapsByRecent(userId, query, pageable);
    }

}
