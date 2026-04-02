package com.zoopzoop.zoopzoop.domain.admin.controller;

import com.zoopzoop.zoopzoop.domain.admin.service.AdminService;
import com.zoopzoop.zoopzoop.standard.dto.HealthCheckDto;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/health")
    public ApiResponse<HealthCheckDto> health() {
        return ApiResponse.ok(adminService.getStatus());
    }
}
