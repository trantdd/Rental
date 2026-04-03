package com.example.rental.mapper;

import com.example.rental.dto.response.BookingResponse;
import com.example.rental.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper chuyá»ƒn Ä‘á»•i giá»¯a Booking entity vÃ  BookingResponse DTO.
 */
@Mapper(componentModel = "spring")
public interface BookingMapper {

    /**
     * Chuyá»ƒn Ä‘á»•i Booking entity thÃ nh BookingResponse DTO.
     * status Ä‘Æ°á»£c map tá»« enum â†’ String tá»± Ä‘á»™ng bá»Ÿi MapStruct.
     */
    @Mapping(target = "status", expression = "java(booking.getStatus().name())")
    BookingResponse toBookingResponse(Booking booking);
}
