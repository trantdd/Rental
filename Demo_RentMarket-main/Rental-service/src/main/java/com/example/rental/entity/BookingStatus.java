package com.example.rental.entity;

/**
 * Trạng thái của một yêu cầu đặt thuê (Booking) mô hình Escrow.
 */
public enum BookingStatus {
    PENDING_PAYMENT,       // Chua thanh toan
    PAID_WAITING_APPROVAL, // Da thanh toan nạp tiền, cho chu do duyet
    APPROVED,              // Chu do da duyet
    IN_PROGRESS,           // Da nhan do, dang thue
    COMPLETED,             // Da tra do, hoan tat
    REJECTED,              // Chu do tu choi
    CANCELLED              // Khach huy
}
