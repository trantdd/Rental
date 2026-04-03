package com.example.rental.mapper;

import com.example.rental.dto.response.PaymentResponse;
import com.example.rental.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentResponse toPaymentResponse(Payment payment);
}
