package com.zoopzoop.zoopzoop.domain.policy.controller;

import com.zoopzoop.zoopzoop.domain.policy.dto.MyScrapIdsResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.MyScrapPageResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyDetailResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyDetailResultDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyPageResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyTypeCountResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.ScrapStatusResponse;
import com.zoopzoop.zoopzoop.domain.policy.service.MyScrapService;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicyQueryService;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySearchService;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySyncService;
import com.zoopzoop.zoopzoop.domain.searchlog.dto.RecentSearchesResponse;
import com.zoopzoop.zoopzoop.domain.searchlog.service.SearchLogService;
import com.zoopzoop.zoopzoop.global.security.AuthenticatedUser;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@Validated
public class PolicyController {

    private final PolicySyncService policySyncService;
    private final PolicyQueryService policyQueryService;
    private final PolicySearchService policySearchService;
    private final SearchLogService searchLogService;
    private final MyScrapService myScrapService;

    @GetMapping
    public ApiResponse<PolicyPageResponse> getPolicies(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String special,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "views") String sort
    ) {
        PolicyPageResponse response = policyQueryService.getPolicies(query, type, age, region, special, page, size, sort);
        if (page == 0) {
            searchLogService.logSearch(user, query);
        }
        return ApiResponse.ok(response);
    }

    @GetMapping("/types")
    public ApiResponse<List<PolicyTypeCountResponse>> getPolicyTypeCounts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String special
    ) {
        return ApiResponse.ok(policyQueryService.getPolicyTypeCounts(query, age, region, special));
    }

    @GetMapping("/recent-searches")
    public ApiResponse<RecentSearchesResponse> getRecentSearches(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(searchLogService.getRecentSearches(user, size));
    }

    @GetMapping("/scraps/me")
    public ApiResponse<MyScrapPageResponse> getMyScraps(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ApiResponse.ok(myScrapService.getMyScraps(user, query, page, size));
    }

    @GetMapping("/scraps/me/ids")
    public ApiResponse<MyScrapIdsResponse> getMyScrapIds(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(myScrapService.getMyScrapIds(user));
    }

    @GetMapping("/{serviceId}/scraps/me")
    public ApiResponse<ScrapStatusResponse> getMyScrapStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String serviceId
    ) {
        return ApiResponse.ok(myScrapService.getScrapStatus(user, serviceId));
    }

    @PostMapping("/{serviceId}/scraps")
    public ApiResponse<ScrapStatusResponse> addScrap(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String serviceId
    ) {
        return ApiResponse.ok(myScrapService.addScrap(user, serviceId));
    }

    @DeleteMapping("/{serviceId}/scraps")
    public ApiResponse<ScrapStatusResponse> removeScrap(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String serviceId
    ) {
        return ApiResponse.ok(myScrapService.removeScrap(user, serviceId));
    }

    @GetMapping("/{serviceId}")
    public ApiResponse<PolicyDetailResponse> getPolicyDetail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String serviceId
    ) {
        PolicyDetailResponse response = policyQueryService.getPolicyDetail(serviceId);
        searchLogService.logPolicyView(user, serviceId);
        return ApiResponse.ok(response);
    }

    @GetMapping("/sync")
    public ResponseEntity<String> syncFromGovApi(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int perPage
    ) {
        log.info("Policy sync requested. page={}, perPage={}", page, perPage);
        PolicySyncService.SyncPageResult result = policySyncService.fetchAndSaveAll(page, perPage);
        return ResponseEntity.ok(result.message());
    }

    @GetMapping("/sync-all")
    public ResponseEntity<String> syncAllFromGovApi() {
        log.info("Full policy sync requested.");
        String result = policySyncService.syncFullData();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ApiResponse<List<PolicySearchResultDto>> searchPolicies(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @NotBlank String keyword,
            @RequestParam(required = false) Integer size
    ) {
        List<PolicySearchResultDto> response = policySearchService.searchPolicies(keyword, size);
        searchLogService.logSearch(user, keyword);
        return ApiResponse.ok(response);
    }

    @GetMapping("/search/{serviceId}")
    public ApiResponse<PolicyDetailResultDto> getSearchPolicyDetail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String serviceId
    ) {
        PolicyDetailResultDto response = policySearchService.getPolicyDetail(serviceId);
        searchLogService.logPolicyView(user, serviceId);
        return ApiResponse.ok(response);
    }
}
