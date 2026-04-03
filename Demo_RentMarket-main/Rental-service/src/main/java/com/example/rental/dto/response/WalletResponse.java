package com.example.rental.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO thông tin ví trả về cho Frontend.
 * Endpoint: GET /rental/wallets/me
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletResponse {

    Long id;

    /** Username chủ ví */
    String userId;

    /** Số dư khả dụng — có thể dùng để thanh toán hoặc rút */
    Double availableBalance;

    /** Số dư đang đóng băng (Escrow) — đang trong giao dịch chờ hoàn tất */
    Double frozenBalance;

    /** Tổng số dư = available + frozen */
    Double totalBalance;
}
