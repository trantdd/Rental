package com.example.rental.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO chứa thông tin thống kê dành cho Dashboard Chủ đồ.
 * PERSON-274
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardResponse {
    
    // ===== Doanh thu =====
    Double totalRevenue;        // Tổng doanh thu (Các đơn COMPLETED)
    Double pendingRevenue;      // Doanh thu chờ (Các đơn PAID nhưng chưa COMPLETED)
    
    // ===== Booking =====
    Long totalBookings;         // Tổng số lượng đơn thuê (mọi trạng thái, trừ CANCELLED/REJECTED nếu muốn)
    Long pendingBookings;       // Số đơn chờ duyệt (PENDING)
    Long activeBookings;        // Số đơn đang cho thuê (APPROVED / PAID)
    Long completedBookings;     // Số đơn đã hoàn tất (COMPLETED)
    
    // ===== Sản phẩm (Gọi sang Product-service) =====
    Long totalItems;            // Tổng số đồ đang đăng
    
    // ===== Đánh giá (Gọi sang Review-service) =====
    Double averageRating;       // Điểm đánh giá trung bình
    Long totalReviews;          // Tổng số lượt đánh giá

    /**
     * Doanh thu hoàn tất theo tháng trong năm hiện tại.
     * Key = Tháng (1-12), Value = Doanh thu hoan tat.
     */
    java.util.Map<Integer, Double> monthlyRevenue;
}
