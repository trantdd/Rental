package com.example.rental.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response tra ve sau khi khoi tao thanh toan MoMo thanh cong.
 * Frontend dung payUrl de redirect user sang trang thanh toan MoMo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MoMoPaymentResponse {

    /** ID booking */
    Long bookingId;

    /** Tong tien can thanh toan (VND) */
    Double amount;

    /** orderId gui len MoMo (BOOKING_{bookingId}_{timestamp}) */
    String orderId;

    /** URL de redirect user sang cong thanh toan MoMo */
    String payUrl;

    /** QR code URL (neu co) */
    String qrCodeUrl;

    /** Ma phan hoi tu MoMo (0 = thanh cong) */
    int resultCode;

    /** Thong bao tu MoMo */
    String message;
}
