package com.example.rental.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * DTO trả về thông tin khả dụng của sản phẩm cho khoảng thời gian cụ thể.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AvailabilityResponse {

    /** Tổng số lượng sản phẩm (từ Product-service) */
    Integer totalQuantity;

    /** Số lượng đã đặt (APPROVED) trong khoảng thời gian */
    Integer bookedQuantity;

    /** Số lượng còn trống */
    Integer availableQuantity;

    /** Ngày dự kiến có hàng lại (null nếu còn hàng) */
    LocalDate nextAvailableDate;
}
