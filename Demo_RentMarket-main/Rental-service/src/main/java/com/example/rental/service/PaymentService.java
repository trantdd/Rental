package com.example.rental.service;

import com.example.rental.dto.request.PaymentRequest;
import com.example.rental.dto.response.PaymentResponse;

/**
 * Service xử lý thanh toán (Mô phỏng).
 * PERSON-270
 */
public interface PaymentService {

    /**
     * Mô phỏng quá trình thanh toán cho Booking.
     * Cập nhật trạng thái Booking thành PAID nếu thành công.
     * 
     * @param request Tải trọng chứa bookingId và số tiền.
     * @return PaymentResponse thông báo kết quả.
     */
    PaymentResponse processPayment(PaymentRequest request);

    /**
     * Lấy chi tiết lịch sử biên lai thanh toán của một Booking.
     */
    PaymentResponse getPaymentByBookingId(Long bookingId);
}
