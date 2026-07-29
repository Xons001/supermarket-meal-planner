package com.sean.supermarketmealplanner.dashboard.infrastructure.web;

import com.sean.supermarketmealplanner.dashboard.application.DashboardResponse;
import com.sean.supermarketmealplanner.dashboard.application.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated user's dashboard projections")
    public DashboardResponse get() {
        return service.get();
    }
}
