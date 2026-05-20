package com.gentlemanstore.analytics.controller;

import com.gentlemanstore.analytics.dto.AnalyticsDTO;
import com.gentlemanstore.analytics.service.AnalyticsService;
import com.gentlemanstore.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService service;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AnalyticsDTO>> getDashboard(){
        return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved successfully", service.getDashboardAnalytics()));
    }
}
