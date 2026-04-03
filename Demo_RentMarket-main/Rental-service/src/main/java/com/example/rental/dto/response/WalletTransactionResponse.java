package com.example.rental.dto.response;

import com.example.rental.entity.TransactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * DTO lịch sử giao dịch ví — trả về cho Frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletTransactionResponse {

    Long id;

    Double amount;

    TransactionType type;

    Long bookingId;

    String description;

    LocalDateTime createdAt;
}
