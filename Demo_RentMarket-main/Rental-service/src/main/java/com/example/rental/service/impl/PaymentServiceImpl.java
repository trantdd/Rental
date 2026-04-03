package com.example.rental.service.impl;

import com.example.rental.config.JwtUtils;
import com.example.rental.dto.request.PaymentRequest;
import com.example.rental.dto.response.PaymentResponse;
import com.example.rental.entity.Booking;
import com.example.rental.entity.BookingStatus;
import com.example.rental.entity.Payment;
import com.example.rental.exception.AppException;
import com.example.rental.exception.ErrorCode;
import com.example.rental.mapper.PaymentMapper;
import com.example.rental.repository.BookingRepository;
import com.example.rental.repository.PaymentRepository;
import com.example.rental.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ThÃ¡Â»Â±c thi cÃƒÂ¡c logic thanh toÃƒÂ¡n (Mock Card).
 * Ã„ÂÃ¡Â»Æ’ xÃ¡Â»Â­ lÃƒÂ½ MoMo, xem MoMoPaymentService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper paymentMapper;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        String currentUser = jwtUtils.getCurrentUsername();
        log.info("Xử lý thanh toán cho booking {} từ user {}", request.getBookingId(), currentUser);

        // 1. Kiểm tra booking có tồn tại và thuộc về user này
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getTenantId().equals(currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // 2. Kiểm tra trạng thái Booking — chỉ thanh toán được khi PENDING_PAYMENT
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }

        // 3. Kiểm tra xem đã thanh toán thành công trước đó chưa
        if (paymentRepository.existsByBookingIdAndStatus(booking.getId(), "SUCCESS")) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_PAID); // Cần thêm error code
        }

        // 4. So lệch số tiền giả lập
        double expectedAmount = booking.getTotalPrice();
        if (Math.abs(request.getAmount() - expectedAmount) > 0.01) {
            log.error("Số tiền thanh toán ({}) không khớp với tổng tiền đơn thuê ({}).", request.getAmount(), expectedAmount);
            // Giả lập thanh toán lỗi
            Payment failedPayment = Payment.builder()
                    .bookingId(booking.getId())
                    .tenantId(currentUser)
                    .amount(request.getAmount())
                    .status("FAILED")
                    .paymentMethod(request.getPaymentMethod())
                    .build();
            paymentRepository.save(failedPayment);
            
            throw new RuntimeException("Số tiền thanh toán không khớp! Yêu cầu: " + expectedAmount);
        }

        // 5. Thanh toán thành công (Lưu Payment & Cập nhật Booking sang PAID)
        Payment successPayment = Payment.builder()
                .bookingId(booking.getId())
                .tenantId(currentUser)
                .amount(expectedAmount)
                .status("SUCCESS")
                .paymentMethod(maskCardNumber(request.getPaymentMethod()))
                .build();
        
        successPayment = paymentRepository.save(successPayment);

        booking.setStatus(BookingStatus.PAID_WAITING_APPROVAL);
        bookingRepository.save(booking);

        log.info("Thanh toán Booking {} thành công!", booking.getId());

        return paymentMapper.toPaymentResponse(successPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        // Có thể cần verify xem người gọi có quyền xem bill này không, tạm bỏ qua để đơn giản cho UI
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu thanh toán cho booking này"));

        return paymentMapper.toPaymentResponse(payment);
    }

    /**
     * Giấu bớt số thẻ VISA/Master giả lập.
     */
    private String maskCardNumber(String raw) {
        if (raw == null || raw.length() < 5) return raw;
        String last4 = raw.substring(raw.length() - 4);
        return "**** **** **** " + last4;
    }
}
