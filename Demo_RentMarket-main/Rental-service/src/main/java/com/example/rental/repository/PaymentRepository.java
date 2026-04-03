package com.example.rental.repository;

import com.example.rental.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository xử lý truy xuất dữ liệu thanh toán.
 * PERSON-268
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * Tìm Payment dựa theo Booking ID.
     */
    Optional<Payment> findByBookingId(Long bookingId);
    
    /**
     * Kiểm tra xem Booking này đã thanh toán chưa.
     */
    boolean existsByBookingIdAndStatus(Long bookingId, String status);
}
