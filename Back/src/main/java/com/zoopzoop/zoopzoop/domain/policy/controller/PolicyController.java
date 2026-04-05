package com.zoopzoop.zoopzoop.domain.policy.controller;

import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyDetailResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyPageResponse;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyTypeCountResponse;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySyncService;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicyQueryService;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j // 💡 로그 사용을 위한 어노테이션
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicySyncService policySyncService;
    private final PolicyQueryService policyQueryService;

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

    /**
     * 1. 단일 페이지 동기화 테스트용
     * URL: http://localhost:8080/api/policies/sync?page=1&perPage=50
     */
    @GetMapping("/sync")
    public ResponseEntity<String> syncFromGovApi(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int perPage) {

        log.info("단일 페이지 동기화 요청 수신 (page: {}, perPage: {})", page, perPage);
        String result = policySyncService.fetchAndSaveAll(page, perPage);
        return ResponseEntity.ok(result);
    }

    /**
     * 2. 전체 데이터 자동 수집 (무한루프)
     * URL: http://localhost:8080/api/policies/sync-all
     */
    @GetMapping("/sync-all")
    public ResponseEntity<String> syncAllFromGovApi() {

        log.info("전체 데이터 동기화 요청 수신!");
        String result = policySyncService.syncFullData();
        return ResponseEntity.ok(result);
    }
}
