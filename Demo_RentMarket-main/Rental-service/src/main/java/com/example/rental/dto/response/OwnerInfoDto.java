package com.example.rental.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO chứa thông tin tóm tắt về chủ đồ (owner).
 * Được đính kèm vào BookingResponse để người thuê có thể liên hệ.
 * Dữ liệu được lấy best-effort từ Identity-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OwnerInfoDto {

    /** Username của chủ đồ (luôn có, lấy từ Booking.productOwnerId) */
    String username;

    /** Họ tên đầy đủ (nullable — lấy từ Identity-service) */
    String fullName;

    /** Email liên hệ (nullable — lấy từ Identity-service) */
    String email;

    /** Số điện thoại liên hệ (nullable — lấy từ Identity-service) */
    String phone;

    /** Địa chỉ (nullable — lấy từ Identity-service) */
    String address;
}
