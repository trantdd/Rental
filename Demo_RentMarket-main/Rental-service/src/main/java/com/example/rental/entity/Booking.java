package com.example.rental.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho một yêu cầu đặt thuê sản phẩm.
 *
 * Mối quan hệ:
 * - tenantId         : username của người thuê (lấy từ JWT, không nhận từ client)
 * - productId        : ID sản phẩm bên Product-service
 * - productOwnerId   : username chủ sở hữu sản phẩm (denormalized để tránh gọi lại Product-service)
 *
 * Luồng trạng thái: PENDING → APPROVED / REJECTED / CANCELLED → COMPLETED
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /**
     * Username của người thuê (JWT sub claim).
     * Không được phép trùng với productOwnerId.
     */
    @Column(nullable = false)
    String tenantId;

    /**
     * ID sản phẩm cần thuê (khớp với Item.id bên Product-service).
     */
    @Column(nullable = false)
    Long productId;

    /**
     * Username của chủ sở hữu sản phẩm.
     * Được lấy từ Product-service khi tạo booking và lưu lại để phục vụ
     * query "listing của tôi" mà không cần gọi inter-service thêm.
     */
    @Column(nullable = false)
    String productOwnerId;

    /**
     * Ngày bắt đầu thuê (tính cả ngày này).
     * Phải >= ngày hôm nay.
     */
    @Column(nullable = false)
    LocalDate startDate;

    /**
     * Ngày kết thúc thuê (tính cả ngày này).
     * Phải sau startDate.
     */
    @Column(nullable = false)
    LocalDate endDate;

    /**
     * Giá thuê mỗi ngày tại thời điểm đặt (snapshot, VNĐ).
     * Lưu lại để tránh trường hợp chủ đồ thay đổi giá sau khi booking.
     */
    @Column(nullable = false)
    Double pricePerDay;

    /**
     * Số ngày thuê = endDate - startDate (tối thiểu 1).
     */
    @Column(nullable = false)
    Integer rentalDays;

    /**
     * Tổng tiền thuê = rentalDays * pricePerDay.
     * Được tính tự động bởi PriceCalculator, không nhận từ client.
     */
    @Column(nullable = false)
    Double rentalFee;

    /**
     * Tiền cọc (hoàn lại 100% khi hoàn tất).
     */
    @Column(nullable = false)
    @Builder.Default
    Double depositFee = 0.0;

    /** Số lượng thuê (mặc định 1) */
    @Column(nullable = false)
    @Builder.Default
    Integer quantity = 1;

    /**
     * Trạng thái booking. Mặc định là PENDING khi tạo mới.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    BookingStatus status;

    /**
     * Ghi chú của người thuê (tuỳ chọn).
     */
    @Column(length = 500)
    String note;

    /**
     * Lý do từ chối của chủ đồ (chỉ có khi status = REJECTED).
     * Giúp người thuê hiểu được nguyên nhân và cải thiện yêu cầu.
     */
    @Column(length = 500)
    String rejectionReason;

    /**
     * Lý do huỷ của người thuê (chỉ có khi status = CANCELLED, tuỳ chọn).
     */
    @Column(length = 500)
    String cancellationReason;

    /**
     * Thời điểm tạo booking. Được set tự động trong @PrePersist.
     */
    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    /**
     * Tự động set trạng thái PENDING_PAYMENT và thời gian tạo khi insert.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = BookingStatus.PENDING_PAYMENT;
        }
    }

    public Double getTotalPrice() {
        return (this.rentalFee != null ? this.rentalFee : 0.0) + (this.depositFee != null ? this.depositFee : 0.0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
