package com.example.rental.controller;

import com.example.rental.dto.response.AdminDashboardResponse;
import com.example.rental.dto.response.ApiResponse;
import com.example.rental.dto.response.DashboardResponse;
import com.example.rental.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller Dashboard — chia thành 2 endpoint riêng biệt:
 *   GET /rental/dashboard/stats        → Thống kê cá nhân của Owner (mọi user đăng nhập)
 *   GET /rental/dashboard/admin/stats  → Thống kê toàn sàn (chỉ ADMIN)
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Thống kê cá nhân của Chủ đồ (Owner Dashboard).
     * Trả về dữ liệu dựa trên JWT của user hiện tại.
     */
    @GetMapping("/stats")
    public ApiResponse<DashboardResponse> getOwnerStats() {
        return ApiResponse.<DashboardResponse>builder()
                .result(dashboardService.getStats())
                .build();
    }

    /**
     * Thống kê toàn sàn (Admin Dashboard).
     * Chỉ user có scope ADMIN mới được phép gọi endpoint này.
     * Nếu dự án chưa có @PreAuthorize, chú thích này sẵn sàng khi bật Spring Security Method Security.
     */
    @GetMapping("/admin/stats")
    // @PreAuthorize("hasAuthority('ADMIN')")   // Bật khi Spring Method Security được enable
    public ApiResponse<AdminDashboardResponse> getAdminStats() {
        return ApiResponse.<AdminDashboardResponse>builder()
                .result(dashboardService.getAdminStats())
                .build();
    }
}
