package com.example.rental.service;

import com.example.rental.dto.response.AdminDashboardResponse;
import com.example.rental.dto.response.DashboardResponse;

/**
 * Service xử lý thống kê doanh thu và báo cáo tổng quan.
 */
public interface DashboardService {

    /** Thống kê cá nhân cho Chủ đồ (Owner) — dựa theo JWT hiện tại. */
    DashboardResponse getStats();

    /** Thống kê toàn sàn dành riêng cho Admin. */
    AdminDashboardResponse getAdminStats();
}
