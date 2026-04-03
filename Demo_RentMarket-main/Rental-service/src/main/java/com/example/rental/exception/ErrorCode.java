package com.example.rental.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import lombok.Getter;

/**
 * Mã lỗi tập trung cho Rental-service.
 * Mỗi mã mang: số code, thông điệp tiếng Việt, và HTTP status tương ứng.
 */
@Getter
public enum ErrorCode {

    // ===== Lỗi hệ thống =====
    INVALID_KEY(1001, "Sai key Enum", HttpStatus.BAD_REQUEST),
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_DATA(1008, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),

    // ===== Lỗi xác thực / phân quyền =====
    UNAUTHENTICATED(1010, "Chưa đăng nhập hoặc token không hợp lệ", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED_ACCESS(1009, "Không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),

    // ===== Lỗi Booking =====
    RENTAL_NOT_FOUND(3012, "Không tìm thấy yêu cầu thuê", HttpStatus.NOT_FOUND),
    BOOKING_NOT_FOUND(3001, "Không tìm thấy booking", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_AVAILABLE(3002, "Sản phẩm không có sẵn để thuê", HttpStatus.CONFLICT),
    INVALID_DATE(3003, "Ngày kết thúc phải sau ngày bắt đầu", HttpStatus.BAD_REQUEST),
    CANNOT_RENT_OWN_PRODUCT(3005, "Bạn không thể đặt thuê sản phẩm của chính mình", HttpStatus.BAD_REQUEST),
    DATE_CONFLICT(3006, "Sản phẩm đã có lịch đặt trong khoảng thời gian này", HttpStatus.CONFLICT),
    BOOKING_NOT_PENDING(3007, "Chỉ có thể cập nhật booking ở trạng thái PENDING", HttpStatus.BAD_REQUEST),
    BOOKING_NOT_APPROVED(3008, "Chỉ có thể hoàn tất booking đã được chấp nhận (APPROVED)", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_QUANTITY(3009, "Số lượng yêu cầu vượt quá số lượng có sẵn", HttpStatus.CONFLICT),
    
    // ===== Lỗi Payment =====
    INVALID_BOOKING_STATUS(3010, "Trạng thái đơn hàng không hợp lệ cho thao tác này", HttpStatus.BAD_REQUEST),
    BOOKING_ALREADY_PAID(3011, "Đơn hàng này đã được thanh toán", HttpStatus.CONFLICT),
    PAYMENT_AMOUNT_MISMATCH(3013, "Số tiền thanh toán không khớp với tổng tiền đơn thuê", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_FOUND(3014, "Không tìm thấy thông tin thanh toán cho booking này", HttpStatus.NOT_FOUND),
    MOMO_PAYMENT_FAILED(3015, "Không thể khởi tạo thanh toán MoMo, vui lòng thử lại", HttpStatus.BAD_GATEWAY),
    WALLET_NOT_FOUND(1015, "Không tìm thấy ví", HttpStatus.NOT_FOUND),
    INSUFFICIENT_BALANCE(3016, "Số dư ví không đủ để thực hiện giao dịch này", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode httpStatusCode;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
