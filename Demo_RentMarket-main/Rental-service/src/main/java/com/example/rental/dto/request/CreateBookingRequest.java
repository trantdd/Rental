package com.example.rental.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * DTO yêu cầu tạo booking thuê sản phẩm — PERSON-176.
 *
 * Lưu ý: tenantId KHÔNG nằm trong DTO này — sẽ được lấy tự động từ JWT.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateBookingRequest {

    /**
     * ID sản phẩm muốn thuê (bắt buộc).
     */
    @NotNull(message = "Vui lòng chọn sản phẩm muốn thuê")
    Long productId;

    /**
     * Ngày bắt đầu thuê (bắt buộc, phải là hôm nay hoặc trong tương lai).
     */
    @NotNull(message = "Vui lòng chọn ngày bắt đầu thuê")
    @FutureOrPresent(message = "Ngày bắt đầu phải là hôm nay hoặc trong tương lai")
    LocalDate startDate;

    /**
     * Ngày kết thúc thuê (bắt buộc, phải sau ngày hôm nay).
     * Validation ngày kết thúc > ngày bắt đầu được kiểm tra trong service.
     */
    @NotNull(message = "Vui lòng chọn ngày kết thúc thuê")
    @Future(message = "Ngày kết thúc phải ở trong tương lai")
    LocalDate endDate;

    /**
     * Số lượng muốn thuê (mặc định 1).
     */
    @Min(value = 1, message = "Số lượng phải ít nhất 1")
    @Builder.Default
    Integer quantity = 1;

    /**
     * Ghi chú của người thuê (tuỳ chọn, tối đa 500 ký tự).
     */
    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    String note;
}
