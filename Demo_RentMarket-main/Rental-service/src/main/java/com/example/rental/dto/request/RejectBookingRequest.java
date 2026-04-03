package com.example.rental.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO yêu cầu từ chối đặt thuê — PERSON-208.
 *
 * Chủ đồ bắt buộc phải cung cấp lý do từ chối để người thuê
 * hiểu được nguyên nhân và cải thiện yêu cầu trong lần sau.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RejectBookingRequest {

    /**
     * Lý do từ chối (bắt buộc, tối đa 500 ký tự).
     * Ví dụ: "Sản phẩm đang bảo trì", "Không nhận đặt thuê ngắn hạn dưới 3 ngày"
     */
    @NotBlank(message = "Vui lòng cung cấp lý do từ chối")
    @Size(max = 500, message = "Lý do từ chối không được vượt quá 500 ký tự")
    String reason;
}
