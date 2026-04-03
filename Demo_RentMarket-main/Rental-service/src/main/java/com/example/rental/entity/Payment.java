package com.example.rental.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity lưu thông tin giao dịch thanh toán (Mô phỏng).
 * PERSON-267
 */
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /**
     * ID của Đơn thuê được thanh toán.
     */
    @Column(name = "booking_id", nullable = false, unique = true)
    Long bookingId;

    /**
     * Người thực hiện thanh toán.
     */
    @Column(name = "tenant_id", nullable = false)
    String tenantId;

    /**
     * Số tiền thanh toán.
     */
    @Column(nullable = false)
    Double amount;

    /**
     * Trạng thái thanh toán (SUCCESS, FAILED, PENDING).
     */
    @Column(nullable = false)
    String status;

    /**
     * Mã giao dịch từ phía "ngân hàng".
     */
    @Column(name = "transaction_id", unique = true)
    String transactionId;

    /**
     * Lời nhắn hoặc thẻ tín dụng ẩn (vd: **** **** **** 1234).
     */
    @Column(name = "payment_method")
    String paymentMethod;

    /**
     * Thời gian thanh toán.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (transactionId == null) {
            transactionId = "TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
}
