package com.zoopzoop.zoopzoop.domain.policy.controller;

import com.zoopzoop.zoopzoop.domain.policy.service.PolicyService;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/health")
    public ApiResponse<HealthCheckDto> health() {
        return ApiResponse.ok(policyService.getStatus());
    }
}
