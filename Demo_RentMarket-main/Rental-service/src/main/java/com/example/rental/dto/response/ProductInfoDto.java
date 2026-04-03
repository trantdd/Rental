package com.example.rental.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO chứa thông tin tóm tắt sản phẩm, được lấy từ Product-service
 * và đính kèm vào BookingResponse — PERSON-190.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductInfoDto {

    /** ID sản phẩm */
    Long id;

    /** Tên sản phẩm */
    String name;

    /** Mô tả sản phẩm */
    String description;

    /** Giá thuê theo ngày (VNĐ) */
    Double pricePerDay;

    /** Trạng thái sản phẩm: AVAILABLE / RENTED */
    String status;

    /** Danh mục sản phẩm */
    String categoryName;

    /** URL hình ảnh đại diện sản phẩm */
    String imageUrl;
}
