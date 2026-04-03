package com.example.rental.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO yêu cầu huỷ đặt thuê — PERSON-215.
 *
 * Người thuê có thể (tuỳ chọn) cung cấp lý do huỷ để giúp chủ đồ
 * hiểu và cải thiện trải nghiệm. Không bắt buộc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancelBookingRequest {

    /**
     * Lý do huỷ đặt thuê (tuỳ chọn, tối đa 500 ký tự).
     * Ví dụ: "Tôi thay đổi kế hoạch", "Tìm được sản phẩm khác phù hợp hơn"
     */
    @Size(max = 500, message = "Lý do huỷ không được vượt quá 500 ký tự")
    String reason;
}
