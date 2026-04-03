package com.example.rental.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request khoi tao thanh toan MoMo tu Frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MoMoPaymentRequest {

    /** ID cua Booking can thanh toan */
    @NotNull(message = "Thieu ID booking")
    Long bookingId;
}
