package com.example.rental.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request thanh toán giả lập.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentRequest {

    /**
     * ID của hoá đơn đặt chỗ (Booking).
     */
    @NotNull(message = "Thiếu ID booking")
    Long bookingId;

    /**
     * Số tiền truyền lên (để so sánh với DB, check validate giả lập).
     */
    @NotNull(message = "Thiếu số tiền thanh toán")
    Double amount;

    /**
     * Tên hoặc mã thẻ thanh toán (Ví dụ: Visa **** 1234)
     */
    @NotBlank(message = "Thiếu thông tin thẻ thanh toán")
    String paymentMethod;
}
