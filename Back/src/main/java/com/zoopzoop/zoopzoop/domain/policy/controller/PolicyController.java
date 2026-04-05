package com.zoopzoop.zoopzoop.domain.policy.controller;

import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyDetailResultDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.domain.policy.service.GovDataFetcherService;
import com.zoopzoop.zoopzoop.domain.policy.service.PolicySearchService;
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

    private final GovDataFetcherService fetcherService;
    private final PolicySearchService policySearchService;

    @GetMapping("/sync")
    public ResponseEntity<String> syncFromGovApi(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int perPage
    ) {
        log.info("Policy sync requested. page={}, perPage={}", page, perPage);
        String result = fetcherService.fetchAndSaveAll(page, perPage);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sync-all")
    public ResponseEntity<String> syncAllFromGovApi() {
        log.info("Full policy sync requested.");
        String result = fetcherService.syncFullData();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ApiResponse<List<PolicySearchResultDto>> searchPolicies(
            @RequestParam @NotBlank String keyword,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(policySearchService.searchPolicies(keyword, size));
    }

    @GetMapping("/{serviceId}")
    public ApiResponse<PolicyDetailResultDto> getPolicyDetail(@PathVariable String serviceId) {
        return ApiResponse.ok(policySearchService.getPolicyDetail(serviceId));
    }
}
