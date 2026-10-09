package com.dsatracker.controller.admin;

import com.dsatracker.dto.admin.AdminDashboardStatsResponse;
import com.dsatracker.service.admin.AdminDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/stats")
    public AdminDashboardStatsResponse stats() {
        return adminDashboardService.getStats();
    }
}
