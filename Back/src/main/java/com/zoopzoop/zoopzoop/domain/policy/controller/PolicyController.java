package com.zoopzoop.zoopzoop.domain.policy.controller;

import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyDetailResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyDetailResultDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyPageResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyTypeCountResponse;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicyQueryService;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySearchService;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySyncService;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping
    public ApiResponse<PolicyPageResponse> getPolicies(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String special,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        return ApiResponse.ok(policyQueryService.getPolicies(query, type, age, region, special, page, size));
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

    @GetMapping("/{serviceId}")
    public ApiResponse<PolicyDetailResponse> getPolicyDetail(@PathVariable String serviceId) {
        return ApiResponse.ok(policyQueryService.getPolicyDetail(serviceId));
    }

    @GetMapping("/sync")
    public ResponseEntity<String> syncFromGovApi(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int perPage
    ) {
        log.info("Policy sync requested. page={}, perPage={}", page, perPage);
        String result = policySyncService.fetchAndSaveAll(page, perPage);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sync-all")
    public ResponseEntity<String> syncAllFromGovApi() {
        log.info("Full policy sync requested.");
        String result = policySyncService.syncFullData();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ApiResponse<List<PolicySearchResultDto>> searchPolicies(
            @RequestParam @NotBlank String keyword,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(policySearchService.searchPolicies(keyword, size));
    }

    @GetMapping("/search/{serviceId}")
    public ApiResponse<PolicyDetailResultDto> getSearchPolicyDetail(@PathVariable String serviceId) {
        return ApiResponse.ok(policySearchService.getPolicyDetail(serviceId));
    }
}
