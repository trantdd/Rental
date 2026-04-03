package com.example.rental.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO thống kê dành riêng cho Admin — tổng quan toàn sàn.
 * Endpoint: GET /rental/dashboard/admin/stats
 * Chỉ user có ROLE ADMIN mới được truy cập.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminDashboardResponse {

    // ===== Doanh thu toàn sàn =====
    /** Tổng phí nền tảng thu được (30% của mỗi đơn COMPLETED) */
    Double totalPlatformRevenue;

    /** Doanh thu nền tảng theo tháng trong năm hiện tại */
    java.util.Map<Integer, Double> monthlyPlatformRevenue;

    // ===== Người dùng =====
    /** Tổng số tài khoản trên hệ thống (lấy từ Identity-service) */
    Long totalUsers;

    // ===== Giao dịch =====
    /** Tổng số booking trên toàn sàn (mọi trạng thái) */
    Long totalTransactions;

    /** Số booking đang chờ duyệt toàn sàn (PAID_WAITING_APPROVAL) */
    Long pendingApprovalCount;

    /** Số booking đã hoàn tất toàn sàn (COMPLETED) */
    Long completedTransactions;

    /** Số booking đã bị hủy/từ chối */
    Long cancelledTransactions;
}
