package com.example.rental.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO chứa thông tin tóm tắt về người thuê (tenant).
 * Được đính kèm vào BookingResponse khi chủ đồ xem danh sách booking — PERSON-197.
 *
 * tenantId (username) luôn có sẵn từ Booking entity.
 * Các thông tin chi tiết hơn (firstName, lastName, email) được lấy best-effort
 * từ Identity-service và sẽ là null nếu service không phản hồi.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TenantInfoDto {

    /** Username của người thuê (luôn có, lấy từ Booking.tenantId) */
    String username;

    /** Họ tên đầy đủ (nullable — lấy từ Identity-service, best-effort) */
    String fullName;

    /** Email (nullable — lấy từ Identity-service, best-effort) */
    String email;

    /** Số điện thoại (nullable — lấy từ Identity-service, best-effort) */
    String phone;

    /** Địa chỉ (nullable — lấy từ Identity-service, best-effort) */
    String address;
}
