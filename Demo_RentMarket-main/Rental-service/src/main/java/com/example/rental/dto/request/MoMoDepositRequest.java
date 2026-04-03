package com.example.rental.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request nạp tiền vào ví qua MoMo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MoMoDepositRequest {

    @NotNull(message = "Thiếu số tiền nạp")
    @Min(value = 10000, message = "Số tiền nạp tối thiểu là 10,000₫")
    Long amount;
}
