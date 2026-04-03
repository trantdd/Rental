package com.example.rental.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Trả về chi tiết kết quả thanh toán.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
    Long id;
    Long bookingId;
    String tenantId;
    Double amount;
    String status; // THÀNH CÔNG, THẤT BẠI
    String transactionId;
    String paymentMethod;
    LocalDateTime createdAt;
}
