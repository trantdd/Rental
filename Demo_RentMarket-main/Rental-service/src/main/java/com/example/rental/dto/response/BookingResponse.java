package com.example.rental.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin một yêu cầu đặt thuê — PERSON-177.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingResponse {

    /** ID booking */
    Long id;

    /** Username của người thuê */
    String tenantId;

    /** ID sản phẩm được thuê */
    Long productId;

    /** Username chủ sở hữu sản phẩm */
    String productOwnerId;

    /** Ngày bắt đầu thuê */
    LocalDate startDate;

    /** Ngày kết thúc thuê */
    LocalDate endDate;

    /** Giá thuê mỗi ngày tại thời điểm đặt (VNĐ) */
    Double pricePerDay;

    /** Số ngày thuê */
    Integer rentalDays;

    /** Tổng tiền thuê (VNĐ) */
    Double rentalFee;

    /** Tiền cọc */
    Double depositFee;

    /** Số lượng thuê */
    Integer quantity;

    /** Trạng thái booking */
    String status;

    /** Ghi chú của người thuê */
    String note;

    /** Lý do từ chối (chỉ có khi status = REJECTED) */
    String rejectionReason;

    /** Lý do huỷ (chỉ có khi status = CANCELLED, tuỳ chọn) */
    String cancellationReason;

    /** Thời điểm tạo booking */
    LocalDateTime createdAt;

    /**
     * Thông tin sản phẩm được thuê (nullable).
     * Được lấy từ Product-service và đính kèm vào response.
     */
    ProductInfoDto productInfo;

    /**
     * Thông tin người thuê (chỉ có trong my-items của chủ đồ, nullable).
     * Được đính kèm để chủ đồ biết ai đang yêu cầu thuê — PERSON-197.
     */
    TenantInfoDto tenantInfo;

    /**
     * Thông tin chủ đồ (chỉ có trong my-rentals của người thuê, nullable).
     * Được đính kèm để người thuê có thể liên hệ chủ đồ.
     */
    OwnerInfoDto ownerInfo;
}
