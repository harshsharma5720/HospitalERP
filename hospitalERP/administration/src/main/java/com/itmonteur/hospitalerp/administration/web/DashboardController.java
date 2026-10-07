package com.itmonteur.hospitalerp.administration.web;

import com.itmonteur.hospitalerp.administration.internal.DashboardDTO;
import com.itmonteur.hospitalerp.administration.internal.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The admin dashboard's figures (docs/ADMIN_DASHBOARD_PLAN.md). Under /api/admin, so admins only. */
@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** {@code days}: 7, 30 or 90 (the period ends today); anything else is a 400. */
    @GetMapping
    public ResponseEntity<DashboardDTO> dashboard(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(dashboardService.dashboard(days));
    }
}
